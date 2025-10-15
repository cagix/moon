(ns cdq.world
  (:require cdq.entity.animation
            cdq.entity.body
            cdq.entity.delete-after-duration
            cdq.entity.projectile-collision
            cdq.entity.stats
            cdq.entity.fsm
            cdq.entity.inventory
            cdq.entity.skills
            [cdq.world.create.grid]
            [cdq.world.assoc-entity-spawn-schema :as assoc-entity-spawn-schema]
            [cdq.world.content-grid :as content-grid]
            [cdq.world.create-fsms :as create-fsms]
            [cdq.world.grid :as grid]
            [cdq.world.grid.cell :as cell]
            [cdq.world.tick-entities :as tick-entities]
            [cdq.world.raycaster :as raycaster]
            [cdq.world.update-potential-fields :as update-potential-fields]
            [cdq.world.tiled :as tiled]
            [clojure.gdx.maps.map-properties :as props]
            [clojure.gdx.maps.tiled :as tiled-map]
            [clojure.gdx.utils.disposable :as disposable]
            [clojure.grid2d :as g2d]
            [clojure.utils :as utils]
            [malli.utils :as mu]
            [qrecord.core :as q]))

(defprotocol World
  (cache-active-entities [_])
  (update-potential-fields! [_])
  (tick-entities! [_]))

(defn dispose! [{:keys [world/tiled-map]}]
  (assert tiled-map) ; only dispose after world was created
  (disposable/dispose! tiled-map))

(defn- create-world-grid [width height cell-movement]
  (g2d/create-grid width
                   height
                   (fn [position]
                     (atom (cell/create position (cell-movement position))))))

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
  World
  (cache-active-entities [{:keys [world/content-grid
                                  world/player-eid]
                           :as world}]
    (assoc world
           :world/active-entities
           (content-grid/active-entities content-grid
                                         @player-eid)))

  (update-potential-fields! [this]
    (update-potential-fields/do! this))

  (tick-entities! [this]
    (tick-entities/do! this))
  )

(defn- assoc-state [world {:keys [tiled-map
                                  start-position]}]
  (let [width  (props/get (tiled-map/properties tiled-map) "width")
        height (props/get (tiled-map/properties tiled-map) "height")
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
           :world/mouseover-eid nil)))

(defn- calculate-max-speed
  [{:keys [world/minimum-size
           world/max-delta]
    :as world}]
  (assoc world :world/max-speed (/ minimum-size max-delta)))

(defn- define-render-z-order
  [{:keys [world/z-orders]
    :as world}]
  (assoc world :world/render-z-order (utils/define-order z-orders)))

(defn create [initial-config world-fn-result]
  (-> (merge (map->RWorld {}) initial-config)
      assoc-entity-spawn-schema/step
      create-fsms/step
      calculate-max-speed
      define-render-z-order
      (assoc-state world-fn-result)))

(def destroy-components
  {:entity/destroy-audiovisual
   {:destroy! (fn [audiovisuals-id eid]
                [[:tx/audiovisual
                  (:body/position (:entity/body @eid))
                  audiovisuals-id]])}})

(defn remove-destroyed-entities!
  [{:keys [world/content-grid
           world/entity-ids
           world/grid]}]
  (mapcat
   (fn [eid]
     (let [id (:entity/id @eid)]
       (assert (contains? @entity-ids id))
       (swap! entity-ids dissoc id))
     (content-grid/remove-entity! content-grid eid)
     (grid/remove-from-touched-cells! grid eid)
     (when (:body/collides? (:entity/body @eid))
       (grid/remove-from-occupied-cells! grid eid))
     (mapcat (fn [[k v]]
               (when-let [destroy! (:destroy! (k destroy-components))]
                 (destroy! v eid)))
             @eid))
   (filter (comp :entity/destroyed? deref)
           (vals @entity-ids))))

(defn mouseover-entity
  [{:keys [world/grid
           world/mouseover-eid
           world/player-eid
           world/render-z-order]
    :as world}
   position]
  (let [player @player-eid
        hits (remove #(= (:body/z-order (:entity/body @%)) :z-order/effect)
                     (grid/point->entities grid position))]
    (->> render-z-order
         (utils/sort-by-order hits #(:body/z-order (:entity/body @%)))
         reverse
         (filter #(raycaster/line-of-sight? world player @%))
         first)))

(def ^:private create-fns
  {:entity/animation             cdq.entity.animation/create
   :entity/body                  cdq.entity.body/create
   :entity/delete-after-duration cdq.entity.delete-after-duration/create
   :entity/projectile-collision  cdq.entity.projectile-collision/create
   :entity/stats                 cdq.entity.stats/create})

(defn- create-component [[k v] world]
  (if-let [f (create-fns k)]
    (f v world)
    v))

(def ^:private create!-fns
  {:entity/fsm                             cdq.entity.fsm/create!
   :entity/inventory                       cdq.entity.inventory/create!
   :entity/skills                          cdq.entity.skills/create!})

(defn- after-create-component [[k v] eid world]
  (when-let [f (create!-fns k)]
    (f v eid world)))

(q/defrecord Entity [entity/body])

(defn spawn-entity!
  [{:keys [world/content-grid
           world/entity-ids
           world/grid
           world/id-counter
           world/spawn-entity-schema]
    :as world}
   entity]
  (mu/validate-humanize spawn-entity-schema entity)
  (let [entity (reduce (fn [m [k v]]
                         (assoc m k (create-component [k v] world)))
                       {}
                       entity)
        _ (assert (and (not (contains? entity :entity/id))))
        entity (assoc entity :entity/id (swap! id-counter inc))
        entity (merge (map->Entity {}) entity)
        eid (atom entity)]
    (let [id (:entity/id @eid)]
      (assert (number? id))
      (swap! entity-ids assoc id eid))
    (content-grid/add-entity! content-grid eid)
    ; https://github.com/damn/core/issues/58
    ;(assert (valid-position? grid @eid))
    (grid/set-touched-cells! grid eid)
    (when (:body/collides? (:entity/body @eid))
      (grid/set-occupied-cells! grid eid))
    (mapcat #(after-create-component % eid world) @eid)))
