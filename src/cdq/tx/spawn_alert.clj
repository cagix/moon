(ns cdq.tx.spawn-alert
  (:require [cdq.world :as world]))

(defn do! [position faction duration]
  (world/delayed-alert position faction duration))
