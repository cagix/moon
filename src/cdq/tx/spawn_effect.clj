(ns cdq.tx.spawn-effect)

(defn do!
  [[_ position components]
   {:keys [ctx/config]}]
  [[:tx/spawn-entity
    (assoc components
           :entity/body (assoc (:effect-body-props config) :position position))]])
