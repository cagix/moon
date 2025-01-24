(ns cdq.render.remove-destroyed-entities
  (:require cdq.world))

(defn render [{:keys [cdq.context/entity-ids
                      context/entity-components]
               :as context}]
  (doseq [eid (filter (comp :entity/destroyed? deref)
                      (vals @entity-ids))]
    (cdq.world/remove-entity context eid)
    (doseq [[k v] @eid]
      (cdq.world/destroy! component eid context)))
  context)
