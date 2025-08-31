(ns cdq.world
  (:require [cdq.cell :as cell]
            [cdq.entity :as entity]
            [cdq.grid :as grid]
            [cdq.grid-impl :as grid-impl]
            [cdq.grid2d :as g2d]
            [cdq.raycaster :as raycaster]
            [cdq.malli :as m]
            [cdq.gdx.math.vector2 :as v]
            [cdq.utils :as utils]
            [cdq.world.content-grid :as content-grid]
            [cdq.world.potential-fields.movement :as potential-fields.movement]
            [cdq.world.potential-fields.update :as potential-fields.update]
            [qrecord.core :as q]))

(def ^:private components-schema
  (m/schema [:map {:closed true}
             [:entity/body :some]
             [:entity/image {:optional true} :some]
             [:entity/animation {:optional true} :some]
             [:entity/delete-after-animation-stopped? {:optional true} :some]
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
             [:creature/stats {:optional true} :some]
             [:entity/inventory    {:optional true} :some]
             [:entity/item {:optional true} :some]
             [:entity/projectile-collision {:optional true} :some]]))

(q/defrecord Entity [entity/body])

(declare entity-components)

(defn- context-entity-add! [{:keys [world/entity-ids
                                    world/content-grid
                                    world/grid]}
                            eid]
  (let [id (entity/id @eid)]
    (assert (number? id))
    (swap! entity-ids assoc id eid))
  (content-grid/add-entity! content-grid eid)
  ; https://github.com/damn/core/issues/58
  ;(assert (valid-position? grid @eid))
  (grid/add-entity! grid eid))

(defn- context-entity-remove! [{:keys [world/entity-ids
                                       world/grid]}
                               eid]
  (let [id (entity/id @eid)]
    (assert (contains? @entity-ids id))
    (swap! entity-ids dissoc id))
  (content-grid/remove-entity! eid)
  (grid/remove-entity! grid eid))

(defn- context-entity-moved! [{:keys [world/content-grid
                                      world/grid]}
                              eid]
  (content-grid/position-changed! content-grid eid)
  (grid/position-changed! grid eid))

(defn update-time [{:keys [world/max-delta] :as world} delta-ms]
  (let [delta-ms (min delta-ms max-delta)]
    (-> world
        (assoc :world/delta-time delta-ms)
        (update :world/elapsed-time + delta-ms))))

(defn path-blocked? [{:keys [world/raycaster]} start end width]
  (raycaster/path-blocked? raycaster start end width))

(defn nearest-enemy-distance [{:keys [world/grid]} entity]
  (cell/nearest-entity-distance @(grid/cell grid (mapv int (entity/position entity)))
                                (entity/enemy entity)))

(defn nearest-enemy [{:keys [world/grid]} entity]
  (cell/nearest-entity @(grid/cell grid (mapv int (entity/position entity)))
                       (entity/enemy entity)))

(defn line-of-sight? [{:keys [world/raycaster]}
                      source
                      target]
  (assert raycaster)
  (not (raycaster/blocked? raycaster
                           (entity/position source)
                           (entity/position target))))

(defn npc-effect-ctx [world eid]
  (let [entity @eid
        target (nearest-enemy world entity)
        target (when (and target
                          (line-of-sight? world entity @target))
                 target)]
    {:effect/source eid
     :effect/target target
     :effect/target-direction (when target
                                (v/direction (entity/position entity)
                                             (entity/position @target)))}))

(defn creatures-in-los-of-player
  [{:keys [world/active-entities
           world/player-eid]
    :as world}]
  (->> active-entities
       (filter #(:entity/species @%))
       (filter #(line-of-sight? world @player-eid @%))
       (remove #(:entity/player? @%))))

(defn potential-field-find-direction [{:keys [world/grid]} eid]
  (potential-fields.movement/find-direction grid eid))

(defn spawn-entity!
  [{:keys [world/id-counter]
    :as world}
   components]
  (m/validate-humanize components-schema components) ; check allowed components at constructor (later more added?)
  (assert (and (not (contains? components :entity/id))))
  (let [eid (atom (merge (map->Entity {})
                         (reduce (fn [m [k v]]
                                   (assoc m k (if-let [create (:create (k entity-components))]
                                                (create v world)
                                                v)))
                                 {}
                                 (assoc components :entity/id (swap! id-counter inc)))))]
    ; this is a 'create!' of entity/id & entity/body ?
    ; then also 'moved!' ...
    (context-entity-add! world eid)
    (mapcat (fn [[k v]]
              (when-let [create! (:create! (k entity-components))]
                (create! v eid world)))
            @eid)))

(defn remove-entity! [world eid]
  (context-entity-remove! world eid)
  (mapcat (fn [[k v]]
            (when-let [destroy! (:destroy! (k entity-components))]
              (destroy! v eid world)))
          @eid))

(defn move-entity! [world eid body direction rotate-in-movement-direction?]
  (context-entity-moved! world eid)
  (swap! eid assoc-in [:entity/body :body/position] (:body/position body))
  (when rotate-in-movement-direction?
    (swap! eid assoc-in [:entity/body :body/rotation-angle] (v/angle-from-vector direction)))
  nil)

; # :z-order/flying has no effect for now
; * entities with :z-order/flying are not flying over water,etc. (movement/air)
; because using potential-field for z-order/ground
; -> would have to add one more potential-field for each faction for z-order/flying
; * they would also (maybe) need a separate occupied-cells if they don't collide with other
; * they could also go over ground units and not collide with them
; ( a test showed then flying OVER player entity )
; -> so no flying units for now
(defn- create-creature-body [position {:keys [body/width body/height #_body/flying?]}]
  {:position position
   :width  width
   :height height
   :collides? true
   :z-order :z-order/ground #_(if flying? :z-order/flying :z-order/ground)})

(defn spawn-creature! [world
                       {:keys [position
                               creature-property
                               components]}]
  (assert creature-property)
  (spawn-entity! world
                 (-> creature-property
                     (assoc :entity/body (create-creature-body position
                                                               (:entity/body creature-property)))
                     (assoc :entity/destroy-audiovisual :audiovisuals/creature-die)
                     (utils/safe-merge components))))

(defn- create-explored-tile-corners [tiled-map]
  (atom (g2d/create-grid (:tiled-map/width  tiled-map)
                         (:tiled-map/height tiled-map)
                         (constantly false))))

(q/defrecord World [world/tiled-map
                    world/start-position
                    world/grid
                    world/explored-tile-corners
                    world/content-grid
                    world/raycaster
                    world/potential-field-cache
                    world/factions-iterations
                    world/id-counter
                    world/entity-ids
                    world/elapsed-time
                    world/max-delta
                    world/max-speed
                    world/minimum-size
                    world/z-orders
                    world/render-z-order

                    ; added later
                    world/delta-time
                    world/paused?
                    world/active-entities
                    world/mouseover-eid
                    world/player-eid])

(defn create
  [{:keys [tiled-map
           start-position]
    :as config}]
  (let [grid (grid-impl/create tiled-map)
        z-orders [:z-order/on-ground
                  :z-order/ground
                  :z-order/flying
                  :z-order/effect]
        ; so that at low fps the game doesn't jump faster between frames used @ movement to set a max speed so entities don't jump over other entities when checking collisions
        max-delta 0.04
        ; setting a min-size for colliding bodies so movement can set a max-speed for not
        ; skipping bodies at too fast movement
        ; TODO assert at properties load
        minimum-size 0.39 ; == spider smallest creature size.
        ; set max speed so small entities are not skipped by projectiles
        ; could set faster than max-speed if I just do multiple smaller movement steps in one frame
        max-speed (/ minimum-size max-delta)]
    (merge (map->World {})
           {:world/tiled-map tiled-map
            :world/start-position start-position
            :world/grid grid
            :world/explored-tile-corners (create-explored-tile-corners tiled-map)
            :world/content-grid (content-grid/create (:tiled-map/width  tiled-map)
                                                     (:tiled-map/height tiled-map)
                                                     (:content-grid-cell-size config))
            :world/raycaster (raycaster/create grid)
            :world/potential-field-cache (atom nil)
            :world/factions-iterations (:potential-field-factions-iterations config)
            :world/id-counter (atom 0)
            :world/entity-ids (atom {})
            :world/elapsed-time 0
            :world/max-delta max-delta
            :world/max-speed max-speed
            :world/minimum-size minimum-size
            :world/z-orders z-orders
            :world/render-z-order (utils/define-order z-orders)})))

(defn dispose! [{:keys [world/tiled-map]}]
  (com.badlogic.gdx.utils.Disposable/.dispose tiled-map)) ; TODO tiled/dispose! ?

(defn cache-active-entities [world]
  (assoc world :world/active-entities
         (content-grid/active-entities (:world/content-grid world)
                                       @(:world/player-eid world))))
(defn tick-potential-fields!
  [{:keys [world/factions-iterations
           world/potential-field-cache
           world/grid
           world/active-entities]}]
  (doseq [[faction max-iterations] factions-iterations]
    (potential-fields.update/tick! potential-field-cache
                                   grid
                                   faction
                                   active-entities
                                   max-iterations)))
