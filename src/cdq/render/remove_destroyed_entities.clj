(ns cdq.render.remove-destroyed-entities
  (:require cdq.world))

(defn render [{:keys [cdq.context/entity-ids] :as c}]
  (doseq [eid (filter (comp :entity/destroyed? deref)
                      (vals @entity-ids))]
    (cdq.world/remove-entity c eid)
    (doseq [component @eid]
      (cdq.world/destroy! component eid c)))
  c)
