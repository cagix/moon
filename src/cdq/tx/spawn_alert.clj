(ns cdq.tx.spawn-alert
  (:require [cdq.timer :as timer]))

(defn do!
  [[_ position faction duration]
   {:keys [ctx/elapsed-time]}]
  [[:tx/spawn-effect
    position
    {:entity/alert-friendlies-after-duration
     {:counter (timer/create elapsed-time duration)
      :faction faction}}]])
