(ns cdq.application.create.world
  (:require [cdq.entity.faction :as faction]
            [cdq.malli :as m]
            [cdq.world.content-grid :as content-grid]
            [cdq.world.grid :as grid]
            [cdq.world.grid.cell :as cell]
            [cdq.world :as world]
            [gdl.math.geom :as geom]
            [gdl.math.raycaster :as raycaster]
            [gdl.math.vector2 :as v]
            [gdl.grid2d :as g2d]
            [gdl.position :as position]
            [gdl.utils :as utils]
            [clojure.disposable :as disposable]
            [gdl.maps.tiled :as tiled]
            [reduce-fsm :as fsm]))

(defn- body->occupied-cells
  [grid
   {:keys [body/position
           body/width
           body/height]
    :as body}]
  (if (or (> (float width) 1) (> (float height) 1))
    (g2d/get-cells grid (geom/body->touched-tiles body))
    [(grid (mapv int position))]))

(extend-type gdl.grid2d.VectorGrid
  cdq.world.grid/Grid
  (circle->cells [g2d circle]
    (->> circle
         geom/circle->outer-rectangle
         geom/rectangle->touched-tiles
         (g2d/get-cells g2d)))

  (cells->entities [_ cells]
    (into #{} (mapcat :entities) cells))

  (circle->entities [g2d {:keys [position radius] :as circle}]
    (->> (grid/circle->cells g2d circle)
         (map deref)
         (grid/cells->entities g2d)
         (filter #(geom/overlaps?
                   (geom/circle (position 0) (position 1) radius)
                   (geom/body->gdx-rectangle (:entity/body @%))))))

  (cached-adjacent-cells [g2d cell]
    (if-let [result (:adjacent-cells @cell)]
      result
      (let [result (->> @cell
                        :position
                        position/get-8-neighbours
                        (g2d/get-cells g2d))]
        (swap! cell assoc :adjacent-cells result)
        result)))

  (point->entities [g2d position]
    (when-let [cell (g2d (mapv int position))]
      (filter #(geom/contains? (geom/body->gdx-rectangle (:entity/body @%)) position)
              (:entities @cell))))

  (set-touched-cells! [grid eid]
    (let [cells (g2d/get-cells grid (geom/body->touched-tiles (:entity/body @eid)))]
      (assert (not-any? nil? cells))
      (swap! eid assoc ::touched-cells cells)
      (doseq [cell cells]
        (assert (not (get (:entities @cell) eid)))
        (swap! cell update :entities conj eid))))

  (remove-from-touched-cells! [_ eid]
    (doseq [cell (::touched-cells @eid)]
      (assert (get (:entities @cell) eid))
      (swap! cell update :entities disj eid)))

  (set-occupied-cells! [grid eid]
    (let [cells (body->occupied-cells grid (:entity/body @eid))]
      (doseq [cell cells]
        (assert (not (get (:occupied @cell) eid)))
        (swap! cell update :occupied conj eid))
      (swap! eid assoc ::occupied-cells cells)))

  (remove-from-occupied-cells! [_ eid]
    (doseq [cell (::occupied-cells @eid)]
      (assert (get (:occupied @cell) eid))
      (swap! cell update :occupied disj eid)))

  (valid-position? [g2d {:keys [body/z-order] :as body} entity-id]
    {:pre [(:body/collides? body)]}
    (let [cells* (into [] (map deref) (g2d/get-cells g2d (geom/body->touched-tiles body)))]
      (and (not-any? #(cell/blocked? % z-order) cells*)
           (->> cells*
                (grid/cells->entities g2d)
                (not-any? (fn [other-entity]
                            (let [other-entity @other-entity]
                              (and (not= (:entity/id other-entity) entity-id)
                                   (:body/collides? (:entity/body other-entity))
                                   (geom/overlaps? (geom/body->gdx-rectangle (:entity/body other-entity))
                                                   (geom/body->gdx-rectangle body))))))))))

  (nearest-enemy-distance [grid entity]
    (cell/nearest-entity-distance @(grid (mapv int (:body/position (:entity/body entity))))
                                  (faction/enemy (:entity/faction entity))))

  (nearest-enemy [grid entity]
    (cell/nearest-entity @(grid (mapv int (:body/position (:entity/body entity))))
                         (faction/enemy (:entity/faction entity)))))

(defrecord RCell [position
                  middle
                  adjacent-cells
                  movement
                  entities
                  occupied
                  good
                  evil]
  cell/Cell
  (blocked? [_ z-order]
    (case movement
      :none true
      :air (case z-order
             :z-order/flying false
             :z-order/ground true)
      :all false))

  (blocks-vision? [_]
    (= movement :none))

  (occupied-by-other? [_ eid]
    (some #(not= % eid) occupied))

  (nearest-entity [this faction]
    (-> this faction :eid))

  (nearest-entity-distance [this faction]
    (-> this faction :distance))

  (pf-blocked? [this]
    (cell/blocked? this :z-order/ground)))

(defn- create-grid-cell [position movement]
  {:pre [(#{:none :air :all} movement)]}
  (map->RCell
   {:position position
    :middle (mapv (partial + 0.5) position)
    :movement movement
    :entities #{}
    :occupied #{}}))

(defn- create-world-grid [width height cell-movement]
  (g2d/create-grid width
                   height
                   (fn [position]
                     (atom (create-grid-cell position (cell-movement position))))))

(defn- update-entity! [{:keys [grid cell-w cell-h]} eid]
  (let [{:keys [cdq.content-grid/content-cell
                entity/body]} @eid
        [x y] (:body/position body)
        new-cell (get grid [(int (/ x cell-w))
                            (int (/ y cell-h))])]
    (when-not (= content-cell new-cell)
      (swap! new-cell update :entities conj eid)
      (swap! eid assoc :cdq.content-grid/content-cell new-cell)
      (when content-cell
        (swap! content-cell update :entities disj eid)))))

(defrecord ContentGrid []
  content-grid/ContentGrid
  (add-entity! [this eid]
    (update-entity! this eid))

  (remove-entity! [_ eid]
    (-> @eid
        :cdq.content-grid/content-cell
        (swap! update :entities disj eid)))

  (position-changed! [this eid]
    (update-entity! this eid))

  (active-entities [{:keys [grid]} center-entity]
    (->> (let [idx (-> center-entity
                       :cdq.content-grid/content-cell
                       deref
                       :idx)]
           (cons idx (g2d/get-8-neighbour-positions idx)))
         (keep grid)
         (mapcat (comp :entities deref)))))

(defn- create-content-grid [width height cell-size]
  (map->ContentGrid
   {:grid (g2d/create-grid
           (inc (int (/ width  cell-size)))
           (inc (int (/ height cell-size)))
           (fn [idx]
             (atom {:idx idx,
                    :entities #{}})))
    :cell-w cell-size
    :cell-h cell-size}))

(defn- create-double-ray-endpositions
  [[start-x start-y]
   [target-x target-y]
   path-w]
  {:pre [(< path-w 0.98)]} ; wieso 0.98??
  (let [path-w (+ path-w 0.02) ;etwas gr�sser damit z.b. projektil nicht an ecken anst�sst
        v (v/direction [start-x start-y]
                       [target-y target-y])
        [normal1 normal2] (v/normal-vectors v)
        normal1 (v/scale normal1 (/ path-w 2))
        normal2 (v/scale normal2 (/ path-w 2))
        start1  (v/add [start-x  start-y]  normal1)
        start2  (v/add [start-x  start-y]  normal2)
        target1 (v/add [target-x target-y] normal1)
        target2 (v/add [target-x target-y] normal2)]
    [start1,target1,start2,target2]))

(defn- path-blocked? [raycaster start target path-w]
  (let [[start1,target1,start2,target2] (create-double-ray-endpositions start target path-w)]
    (or
     (raycaster/blocked? raycaster start1 target1)
     (raycaster/blocked? raycaster start2 target2))))

(defn- create-explored-tile-corners [width height]
  (atom (g2d/create-grid width height (constantly false))))

(defn- create-raycaster [g2d]
  (let [width  (g2d/width  g2d)
        height (g2d/height g2d)
        cells  (for [cell (map deref (g2d/cells g2d))]
                 [(:position cell)
                  (boolean (cell/blocks-vision? cell))])]
    (let [arr (make-array Boolean/TYPE width height)]
      (doseq [[[x y] blocked?] cells]
        (aset arr x y (boolean blocked?)))
      [arr width height])))

(defrecord World []
  world/RayCaster
  (ray-blocked? [{:keys [world/raycaster]} start target]
    (raycaster/blocked? raycaster start target))

  (path-blocked? [{:keys [world/raycaster]} start target path-w]
    (path-blocked? raycaster start target path-w))

  (line-of-sight? [{:keys [world/raycaster]} source target]
    (not (raycaster/blocked? raycaster
                             (:body/position (:entity/body source))
                             (:body/position (:entity/body target)))))

  disposable/Disposable
  (dispose! [{:keys [world/tiled-map]}]
    (when tiled-map ; initialization
      (disposable/dispose! tiled-map)))

  world/World
  (active-eids [this]
    (:world/active-entities this))

  world/Resettable
  (reset-state [world {:keys [tiled-map
                              start-position]}]
    (let [width  (:tiled-map/width  tiled-map)
          height (:tiled-map/height tiled-map)
          grid (create-world-grid width height
                                  #(case (tiled/movement-property tiled-map %)
                                     "none" :none
                                     "air"  :air
                                     "all"  :all))]
      (assoc world
             :world/tiled-map tiled-map
             :world/start-position start-position
             :world/grid grid
             :world/content-grid (create-content-grid width height (:content-grid-cell-size world))
             :world/explored-tile-corners (create-explored-tile-corners width height)
             :world/raycaster (create-raycaster grid)
             :world/elapsed-time 0
             :world/potential-field-cache (atom nil)
             :world/id-counter (atom 0)
             :world/entity-ids (atom {})
             :world/paused? false
             :world/mouseover-eid nil))))

(comment

 ; 1. quote the data structur ebecause of arrows
 ; 2.
 (eval `(fsm/fsm-inc ~data))
 )

(def ^:private npc-fsm
  (fsm/fsm-inc
   [[:npc-sleeping
     :kill -> :npc-dead
     :stun -> :stunned
     :alert -> :npc-idle]
    [:npc-idle
     :kill -> :npc-dead
     :stun -> :stunned
     :start-action -> :active-skill
     :movement-direction -> :npc-moving]
    [:npc-moving
     :kill -> :npc-dead
     :stun -> :stunned
     :timer-finished -> :npc-idle]
    [:active-skill
     :kill -> :npc-dead
     :stun -> :stunned
     :action-done -> :npc-idle]
    [:stunned
     :kill -> :npc-dead
     :effect-wears-off -> :npc-idle]
    [:npc-dead]]))

(def ^:private player-fsm
  (fsm/fsm-inc
   [[:player-idle
     :kill -> :player-dead
     :stun -> :stunned
     :start-action -> :active-skill
     :pickup-item -> :player-item-on-cursor
     :movement-input -> :player-moving]
    [:player-moving
     :kill -> :player-dead
     :stun -> :stunned
     :no-movement-input -> :player-idle]
    [:active-skill
     :kill -> :player-dead
     :stun -> :stunned
     :action-done -> :player-idle]
    [:stunned
     :kill -> :player-dead
     :effect-wears-off -> :player-idle]
    [:player-item-on-cursor
     :kill -> :player-dead
     :stun -> :stunned
     :drop-item -> :player-idle
     :dropped-item -> :player-idle]
    [:player-dead]]))

(defn- calculate-max-speed
  [{:keys [world/minimum-size
           world/max-delta]
    :as world}]
  (assoc world :world/max-speed (/ minimum-size max-delta)))

(defn- define-render-z-order
  [{:keys [world/z-orders]
    :as world}]
  (assoc world :world/render-z-order (utils/define-order z-orders)))

(defn- create-fsms
  [world]
  (assoc world :world/fsms {:fsms/player player-fsm
                            :fsms/npc npc-fsm}))

(def ^:private components-schema
  (m/schema [:map {:closed true}
             [:entity/body :some]
             [:entity/image {:optional true} :some]
             [:entity/animation {:optional true} :some]
             [:entity/alert-friendlies-after-duration {:optional true} :some]
             [:entity/line-render {:optional true} :some]
             [:entity/delete-after-duration {:optional true} :some]
             [:entity/destroy-audiovisual {:optional true} :some]
             [:entity/fsm {:optional true} :some]
             [:entity/player? {:optional true} :some]
             [:entity/free-skill-points {:optional true} :some]
             [:entity/click-distance-tiles {:optional true} :some]
             [:entity/clickable {:optional true} :some]
             [:property/id {:optional true} :some]
             [:property/pretty-name {:optional true} :some]
             [:creature/level {:optional true} :some]
             [:entity/faction {:optional true} :some]
             [:entity/species {:optional true} :some]
             [:entity/movement {:optional true} :some]
             [:entity/skills {:optional true} :some]
             [:entity/stats {:optional true} :some]
             [:entity/inventory    {:optional true} :some]
             [:entity/item {:optional true} :some]
             [:entity/projectile-collision {:optional true} :some]]))

(defn- entity-schema [world]
  (assoc world :world/spawn-entity-schema components-schema))

(def ^:private initial-state
  {:content-grid-cell-size 16
   :world/factions-iterations {:good 15 :evil 5}
   :world/max-delta 0.04
   :world/minimum-size 0.39
   :world/z-orders [:z-order/on-ground
                    :z-order/ground
                    :z-order/flying
                    :z-order/effect]
   :world/enemy-components {:entity/fsm {:fsm :fsms/npc
                                         :initial-state :npc-sleeping}
                            :entity/faction :evil}
   :world/player-components {:creature-id :creatures/vampire
                             :components {:entity/fsm {:fsm :fsms/player
                                                       :initial-state :player-idle}
                                          :entity/faction :good
                                          :entity/player? true
                                          :entity/free-skill-points 3
                                          :entity/clickable {:type :clickable/player}
                                          :entity/click-distance-tiles 1.5}}
   :world/effect-body-props {:width 0.5
                             :height 0.5
                             :z-order :z-order/effect}})

(defn do! [ctx]
  (assoc ctx :ctx/world (-> (merge (map->World {}) initial-state)
                            entity-schema
                            create-fsms
                            calculate-max-speed
                            define-render-z-order)))
