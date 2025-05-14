(ns cdq.tx.spawn-line
  (:require [cdq.world :as world]))

(defn do! [opts]
  (world/line-render opts))
