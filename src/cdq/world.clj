(ns cdq.world
  (:require [cdq.content-grid :as content-grid]
            [cdq.grid :as grid]
            [cdq.malli :as m]
            [qrecord.core :as q]))

(q/defrecord Entity [entity/body])

(defn spawn-entity!
  [{:keys [ctx/id-counter
           ctx/entity-ids
           ctx/entity-components
           ctx/spawn-entity-schema
           ctx/content-grid
           ctx/grid]
    :as ctx}
   components]
  (m/validate-humanize spawn-entity-schema components)
  (assert (and (not (contains? components :entity/id))))
  (let [eid (atom (merge (map->Entity {})
                         (reduce (fn [m [k v]]
                                   (assoc m k (if-let [create (:create (k entity-components))]
                                                (create v ctx)
                                                v)))
                                 {}
                                 (assoc components :entity/id (swap! id-counter inc)))))]
    (let [id (:entity/id @eid)]
      (assert (number? id))
      (swap! entity-ids assoc id eid))
    (content-grid/add-entity! content-grid eid)
    ; https://github.com/damn/core/issues/58
    ;(assert (valid-position? grid @eid))
    (grid/add-entity! grid eid)
    (mapcat (fn [[k v]]
              (when-let [create! (:create! (k entity-components))]
                (create! v eid ctx)))
            @eid)))
