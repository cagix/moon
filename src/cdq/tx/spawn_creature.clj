(ns cdq.tx.spawn-creature
  (:require [cdq.w :as w]))

(defn do! [[_ opts] {:keys [ctx/world]}]
  (w/spawn-creature! world opts))
