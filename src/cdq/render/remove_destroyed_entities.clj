(ns cdq.render.remove-destroyed-entities
  (:require cdq.world))

(defn render [{:keys [cdq.context/entity-ids] :as c}]
  ; do not pause this as for example pickup item, should be destroyed => make test & remove comment.
  (doseq [eid (filter (comp :entity/destroyed? deref)
                      (vals @entity-ids))]
    (cdq.world/remove-entity c eid)
    (doseq [component @eid]
      (cdq.world/destroy! component eid c)))
  c)
