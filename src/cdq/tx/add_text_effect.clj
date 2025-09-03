(ns cdq.tx.add-text-effect
  (:require [cdq.entity.timers :as timers]))

(defn do! [[_ eid text duration] {:keys [ctx/world]}]
  (swap! eid timers/add-text-effect text duration (:world/elapsed-time world))
  nil)
