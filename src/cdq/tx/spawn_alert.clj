(ns cdq.tx.spawn-alert
  (:require [cdq.timer :as timer]))

(defn do!
  [[_ position faction duration]
   {:keys [ctx/world]}]
  [[:tx/spawn-effect
    position
    {:entity/alert-friendlies-after-duration
     {:counter (timer/create (:world/elapsed-time world) duration)
      :faction faction}}]])
