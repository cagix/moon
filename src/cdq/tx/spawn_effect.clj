(ns cdq.tx.spawn-effect
  (:require [cdq.w :as w]))

(defn do!
  [[_ position components]
   {:keys [ctx/config
           ctx/world]}]
  (w/spawn-entity! world
                   (assoc components
                          :entity/body (assoc (:effect-body-props config) :position position))))
