(ns cdq.world
  (:require [cdq.cell :as cell]
            [cdq.content-grid :as content-grid]
            [cdq.entity :as entity]
            [cdq.grid :as grid]
            [cdq.raycaster :as raycaster]
            [cdq.malli :as m]
            [cdq.math.vector2 :as v]
            [cdq.potential-fields.movement :as potential-fields.movement]
            [cdq.utils :as utils]
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

(defn- create-component-value
  [world k v]
  (if-let [create (:create (k entity-components))]
    (create v world)
    v))

(defn- create!-component-value
  [world [k v] eid]
  (when-let [create! (:create! (k entity-components))]
    (create! v eid world)))

(defn- component-destroy!
  [world [k v] eid]
  (when-let [destroy! (:destroy! (k entity-components))]
    (destroy! v eid world)))

(defn- create-vs [components world]
  (reduce (fn [m [k v]]
            (assoc m k (create-component-value world k v)))
          {}
          components))

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
  (m/validate-humanize components-schema components)
  (assert (and (not (contains? components :entity/id))))
  (let [eid (atom (merge (map->Entity {})
                         (-> components
                             (assoc :entity/id (swap! id-counter inc))
                             (create-vs world))))]
    (context-entity-add! world eid)
    (mapcat #(create!-component-value world % eid) @eid)))

(defn remove-entity! [world eid]
  (context-entity-remove! world eid)
  (mapcat #(component-destroy! world % eid) @eid))

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
