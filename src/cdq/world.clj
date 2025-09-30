(ns cdq.world
  (:require cdq.world.update-potential-fields
            cdq.potential-fields.movement
            [cdq.creature :as creature]
            [cdq.effect :as effect]
            [cdq.entity.body :as body]
            [cdq.entity.animation :as animation]
            [cdq.entity.faction :as faction]
            [cdq.entity.stats :as stats]
            [cdq.malli :as m]
            [cdq.timer :as timer]
            [cdq.world.content-grid :as content-grid]
            [cdq.world.grid :as grid]
            [cdq.world.grid.cell :as cell]
            [gdl.math.geom :as geom]
            [gdl.math.raycaster :as raycaster]
            [gdl.math.vector2 :as v]
            [gdl.grid2d :as g2d]
            [gdl.position :as position]
            [gdl.utils :as utils]
            [com.badlogic.gdx.utils.disposable :as disposable]
            [com.badlogic.gdx.maps.tiled :as tiled]
            [reduce-fsm :as fsm]))

(defprotocol Resettable
  (reset-state [_ world-fn-result]))

(defprotocol World
  (active-eids [_]))

(defprotocol RayCaster
  (ray-blocked? [_ start target])
  (path-blocked? [_ start target path-w])
  (line-of-sight? [_ source target]))

(defn update-potential-fields! [world]
  (cdq.world.update-potential-fields/do! world))

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

(defn- path-blocked?* [raycaster start target path-w]
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

(defrecord RWorld []
  RayCaster
  (ray-blocked? [{:keys [world/raycaster]} start target]
    (raycaster/blocked? raycaster start target))

  (path-blocked? [{:keys [world/raycaster]} start target path-w]
    (path-blocked?* raycaster start target path-w))

  (line-of-sight? [{:keys [world/raycaster]} source target]
    (not (raycaster/blocked? raycaster
                             (:body/position (:entity/body source))
                             (:body/position (:entity/body target)))))

  disposable/Disposable
  (dispose! [{:keys [world/tiled-map]}]
    (when tiled-map ; initialization
      (disposable/dispose! tiled-map)))

  World
  (active-eids [this]
    (:world/active-entities this))

  Resettable
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

(defn create [initial-state]
  (-> (merge (map->RWorld {}) initial-state)
      entity-schema
      create-fsms
      calculate-max-speed
      define-render-z-order))

(defn- tick-entities!*
  [{:keys [world/active-entities]
    :as world}
   k->tick-fn]
  (mapcat (fn [eid]
            (mapcat (fn [[k v]]
                      (when-let [f (k->tick-fn k)]
                        (f v eid world)))
                    @eid))
          active-entities))

(comment
 (= (tick-entities!* {:world/active-entities [(atom {:firstk :foo
                                                     :secondk :bar})
                                              (atom {:firstk2 :foo2
                                                     :secondk2 :bar2})]}
                     {:firstk (fn [v eid world]
                                [[:foo/bar]])
                      :secondk (fn [v eid world]
                                 [[:foo/barz]
                                  [:asdf]])
                      :firstk2 (fn [v eid world]
                                 nil)
                      :secondk2 (fn [v eid world]
                                  [[:asdf2] [:asdf3]])})
    [[:foo/bar]
     [:foo/barz]
     [:asdf]
     [:asdf2]
     [:asdf3]])
 )

(def movement-ai-logic cdq.potential-fields.movement/find-movement-direction)

(defn- find-movement-direction [{:keys [world/grid]} eid]
  (movement-ai-logic grid eid))

(defn- npc-choose-skill [world entity effect-ctx]
  (->> entity
       :entity/skills
       vals
       (sort-by #(or (:skill/cost %) 0))
       reverse
       (filter #(and (= :usable (creature/skill-usable-state entity % effect-ctx))
                     (->> (:skill/effects %)
                          (filter (fn [e] (effect/applicable? e effect-ctx)))
                          (some (fn [e] (effect/useful? e effect-ctx world))))))
       first))

(defn- npc-effect-ctx
  [{:keys [world/grid]
    :as world}
   eid]
  (let [entity @eid
        target (grid/nearest-enemy grid entity)
        target (when (and target
                          (line-of-sight? world entity @target))
                 target)]
    {:effect/source eid
     :effect/target target
     :effect/target-direction (when target
                                (body/direction (:entity/body entity)
                                                (:entity/body @target)))}))

(defn- update-effect-ctx
  [world {:keys [effect/source effect/target] :as effect-ctx}]
  (if (and target
           (not (:entity/destroyed? @target))
           (line-of-sight? world @source @target))
    effect-ctx
    (dissoc effect-ctx :effect/target)))

(defn- move-position [position {:keys [direction speed delta-time]}]
  (mapv #(+ %1 (* %2 speed delta-time)) position direction))

(defn- move-body [body movement]
  (update body :body/position move-position movement))

(defn- try-move [grid body entity-id movement]
  (let [new-body (move-body body movement)]
    (when (grid/valid-position? grid new-body entity-id)
      new-body)))

(defn- try-move-solid-body [grid body entity-id {[vx vy] :direction :as movement}]
  (let [xdir (Math/signum (float vx))
        ydir (Math/signum (float vy))]
    (or (try-move grid body entity-id movement)
        (try-move grid body entity-id (assoc movement :direction [xdir 0]))
        (try-move grid body entity-id (assoc movement :direction [0 ydir])))))

(def ^:private k->tick-fn
  {:entity/alert-friendlies-after-duration (fn
                                             [{:keys [counter faction]}
                                              eid
                                              {:keys [world/elapsed-time
                                                      world/grid]}]
                                             (when (timer/stopped? elapsed-time counter)
                                               (cons [:tx/mark-destroyed eid]
                                                     (for [friendly-eid (->> {:position (:body/position (:entity/body @eid))
                                                                              :radius 4}
                                                                             (grid/circle->entities grid)
                                                                             (filter #(= (:entity/faction @%) faction)))]
                                                       [:tx/event friendly-eid :alert]))))

   :entity/animation                       (fn [animation eid {:keys [world/delta-time]}]
                                             [[:tx/assoc eid :entity/animation (animation/tick animation delta-time)]
                                              (when (and (:delete-after-stopped? animation)
                                                         (animation/stopped? animation))
                                                [:tx/mark-destroyed eid])])

   :entity/delete-after-duration           (fn [counter eid {:keys [world/elapsed-time]}]
                                             (when (timer/stopped? elapsed-time counter)
                                               [[:tx/mark-destroyed eid]]))

   :entity/movement                        (fn
                                             [{:keys [direction
                                                      speed
                                                      rotate-in-movement-direction?]
                                               :as movement}
                                              eid
                                              {:keys [world/delta-time
                                                      world/grid
                                                      world/max-speed]}]
                                             (assert (<= 0 speed max-speed)
                                                     (pr-str speed))
                                             (assert (or (zero? (v/length direction))
                                                         (utils/nearly-equal? 1 (v/length direction)))
                                                     (str "cannot understand direction: " (pr-str direction)))
                                             (when-not (or (zero? (v/length direction))
                                                           (nil? speed)
                                                           (zero? speed))
                                               (let [movement (assoc movement :delta-time delta-time)
                                                     body (:entity/body @eid)]
                                                 (when-let [body (if (:body/collides? body)
                                                                   (try-move-solid-body grid body (:entity/id @eid) movement)
                                                                   (move-body body movement))]
                                                   [[:tx/move-entity eid body direction rotate-in-movement-direction?]]))))

   :entity/projectile-collision            (fn
                                             [{:keys [entity-effects already-hit-bodies piercing?]}
                                              eid
                                              {:keys [world/grid]}]
                                             (let [entity @eid
                                                   cells* (map deref (g2d/get-cells grid (body/touched-tiles (:entity/body entity))))
                                                   hit-entity (first (filter #(and (not (contains? already-hit-bodies %))
                                                                                   (not= (:entity/faction entity)
                                                                                         (:entity/faction @%))
                                                                                   (:body/collides? (:entity/body @%))
                                                                                   (body/overlaps? (:entity/body entity)
                                                                                                   (:entity/body @%)))
                                                                             (grid/cells->entities grid cells*)))
                                                   destroy? (or (and hit-entity (not piercing?))
                                                                (some #(cell/blocked? % (:body/z-order (:entity/body entity))) cells*))]
                                               [(when destroy?
                                                  [:tx/mark-destroyed eid])
                                                (when hit-entity
                                                  [:tx/assoc-in
                                                   eid
                                                   [:entity/projectile-collision
                                                    :already-hit-bodies]
                                                   (conj already-hit-bodies hit-entity)])
                                                (when hit-entity
                                                  [:tx/effect
                                                   {:effect/source eid
                                                    :effect/target hit-entity}
                                                   entity-effects])]))

   :entity/skills                          (fn [skills eid {:keys [world/elapsed-time]}]
                                             (for [{:keys [skill/cooling-down?] :as skill} (vals skills)
                                                   :when (and cooling-down?
                                                              (timer/stopped? elapsed-time cooling-down?))]
                                               [:tx/assoc-in eid [:entity/skills (:property/id skill) :skill/cooling-down?] false]))

   :active-skill                           (fn
                                             [{:keys [skill effect-ctx counter]}
                                              eid
                                              {:keys [world/elapsed-time]
                                               :as world}]
                                             (let [effect-ctx (update-effect-ctx world effect-ctx)]
                                               (cond
                                                (not (seq (filter #(effect/applicable? % effect-ctx)
                                                                  (:skill/effects skill))))
                                                [[:tx/event eid :action-done]]

                                                (timer/stopped? elapsed-time counter)
                                                [[:tx/effect effect-ctx (:skill/effects skill)]
                                                 [:tx/event eid :action-done]])))

   :npc-idle                               (fn [_ eid world]
                                             (let [effect-ctx (npc-effect-ctx world eid)]
                                               (if-let [skill (npc-choose-skill world @eid effect-ctx)]
                                                 [[:tx/event eid :start-action [skill effect-ctx]]]
                                                 [[:tx/event eid :movement-direction (or (find-movement-direction world eid)
                                                                                         [0 0])]])))

   :npc-moving                             (fn [{:keys [timer]} eid {:keys [world/elapsed-time]}]
                                             (when (timer/stopped? elapsed-time timer)
                                               [[:tx/event eid :timer-finished]]))

   :npc-sleeping                           (fn [_ eid {:keys [world/grid]}]
                                             (let [entity @eid]
                                               (when-let [distance (grid/nearest-enemy-distance grid entity)]
                                                 (when (<= distance (stats/get-stat-value (:entity/stats entity) :stats/aggro-range))
                                                   [[:tx/event eid :alert]]))))

   :stunned                                (fn [{:keys [counter]} eid {:keys [world/elapsed-time]}]
                                             (when (timer/stopped? elapsed-time counter)
                                               [[:tx/event eid :effect-wears-off]]))

   :entity/string-effect                   (fn
                                             [{:keys [counter]}
                                              eid
                                              {:keys [world/elapsed-time]}]
                                             (when (timer/stopped? elapsed-time counter)
                                               [[:tx/dissoc eid :entity/string-effect]]))

   :entity/temp-modifier                   (fn
                                             [{:keys [modifiers counter]}
                                              eid
                                              {:keys [world/elapsed-time]}]
                                             (when (timer/stopped? elapsed-time counter)
                                               [[:tx/dissoc     eid :entity/temp-modifier]
                                                [:tx/mod-remove eid modifiers]]))})

(defn tick-entities! [world]
  (tick-entities!* world k->tick-fn))

(def ^:private k->fn
  '{:effects/audiovisual {:applicable? cdq.effects.audiovisual/applicable?
                          :useful?     cdq.effects.audiovisual/useful?
                          :handle      cdq.effects.audiovisual/handle}

    :effects/projectile {:applicable? cdq.effects.projectile/applicable?
                         :useful?     cdq.effects.projectile/useful?
                         :handle      cdq.effects.projectile/handle}

    :effects/spawn {:applicable? cdq.effects.spawn/applicable?
                    :handle      cdq.effects.spawn/handle}

    :effects/target-all {:applicable? cdq.effects.target-all/applicable?
                         :useful?     cdq.effects.target-all/useful?
                         :handle      cdq.effects.target-all/handle
                         :draw        cdq.effects.target-all/draw}

    :effects/target-entity {:applicable? cdq.effects.target-entity/applicable?
                            :useful?     cdq.effects.target-entity/useful?
                            :handle      cdq.effects.target-entity/handle
                            :draw        cdq.effects.target-entity/draw}

    :effects.target/audiovisual {:applicable? cdq.effects.target.audiovisual/applicable?
                                 :useful?     cdq.effects.target.audiovisual/useful?
                                 :handle      cdq.effects.target.audiovisual/handle}

    :effects.target/convert {:applicable? cdq.effects.target.convert/applicable?
                             :handle      cdq.effects.target.convert/handle}

    :effects.target/damage {:applicable? cdq.effects.target.damage/applicable?
                            :handle      cdq.effects.target.damage/handle}

    :effects.target/kill {:applicable? cdq.effects.target.kill/applicable?
                          :handle      cdq.effects.target.kill/handle}

    :effects.target/melee-damage {:applicable? cdq.effects.target.melee-damage/applicable?
                                  :handle      cdq.effects.target.melee-damage/handle}

    :effects.target/spiderweb {:applicable? cdq.effects.target.spiderweb/applicable?
                               :handle      cdq.effects.target.spiderweb/handle}

    :effects.target/stun {:applicable? cdq.effects.target.stun/applicable?
                          :handle      cdq.effects.target.stun/handle}})

(alter-var-root #'k->fn update-vals
                (fn [k->fn]
                  (update-vals k->fn
                               (fn [sym]
                                 (let [avar (requiring-resolve sym)]
                                   (assert avar sym)
                                   avar)))))

(extend clojure.lang.APersistentVector
  effect/Effect
  {:applicable? (fn [{k 0 :as component} effect-ctx]
                  ((:applicable? (k->fn k)) component effect-ctx))

   :handle (fn [{k 0 :as component} effect-ctx world]
             ((:handle (k->fn k)) component effect-ctx world))

   :useful? (fn [{k 0 :as component} effect-ctx world]
              (if-let [f (:useful? (k->fn k))]
                (f component effect-ctx world)
                true))

   :draw (fn [{k 0 :as component} effect-ctx ctx]
           (if-let [f (:draw (k->fn k))]
             (f component effect-ctx ctx)
             nil))})
