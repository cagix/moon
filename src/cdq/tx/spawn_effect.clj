(ns cdq.tx.spawn-effect)

(defn do!
  [{:keys [ctx/config]}
   position
   components]
  [[:tx/spawn-entity
    (assoc components
           :entity/body (assoc (:effect-body-props config) :position position))]])
