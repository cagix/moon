(ns cdq.tx.spawn-creature
  (:require [cdq.world :as w]))

(defn do! [[_ opts] {:keys [ctx/world]}]
  (w/spawn-creature! world opts))
