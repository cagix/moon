(ns cdq.tx.spawn-alert
  (:require [gdl.timer :as timer]))

(defn do! [{:keys [ctx/world]} position faction duration]
  [[:tx/spawn-effect
    position
    {:entity/alert-friendlies-after-duration
     {:counter (timer/create (:world/elapsed-time world) duration)
      :faction faction}}]])
