(ns cdq.render.remove-destroyed-entities
  (:require [cdq.context :as context]))

(defn render [{:keys [cdq.context/entity-ids
                      context/entity-components]
               :as context}]
  (doseq [eid (filter (comp :entity/destroyed? deref)
                      (vals @entity-ids))]
    (doseq [component context]
      (context/remove-entity component eid))
    (doseq [[k v] @eid
            :let [destroy! (get-in entity-components [k :destroy!])]
            :when destroy!]
      (destroy! v eid context)))
  context)
