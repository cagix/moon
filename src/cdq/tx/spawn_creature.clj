(ns cdq.tx.spawn-creature
  (:require [cdq.world :as world]))

(defn do! [opts]
  (world/spawn-creature opts))
