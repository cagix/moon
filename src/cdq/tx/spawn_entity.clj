(ns cdq.tx.spawn-entity
  (:require [cdq.creature :as creature]
            [cdq.effect :as effect]
            [cdq.entity :as entity]
            [cdq.stats :as modifiers]
            [cdq.world.content-grid :as content-grid]
            [cdq.world.grid :as grid]
            [malli.utils]
            [qrecord.core :as q]))

(q/defrecord Entity [entity/body])

(extend-type Entity
  creature/Skills
  (skill-usable-state [entity
                       {:keys [skill/cooling-down? skill/effects] :as skill}
                       effect-ctx]
    (cond
     cooling-down?
     :cooldown

     (modifiers/not-enough-mana? (:creature/stats entity) skill)
     :not-enough-mana

     (not (effect/some-applicable? effect-ctx effects))
     :invalid-params

     :else
     :usable)))

(defn do!
  [{:keys [ctx/world]
    :as ctx}
   entity]
  (let [{:keys [world/content-grid
                world/entity-ids
                world/grid
                world/id-counter
                world/spawn-entity-schema
                ]} world
        _ (malli.utils/validate-humanize spawn-entity-schema entity)
        entity (reduce (fn [m [k v]]
                         (assoc m k (entity/create [k v] ctx)))
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
    (mapcat #(entity/create! % eid ctx) @eid)))
