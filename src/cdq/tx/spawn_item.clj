(ns cdq.tx.spawn-item
  (:require [cdq.world :as world]))

(defn do! [position item]
  (world/spawn-item position item))
