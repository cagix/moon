(ns cdq.tx.spawn-alert
  (:require [cdq.timer :as timer]
            [cdq.g :as g]))

(defn do! [{:keys [ctx/elapsed-time] :as ctx}
           position
           faction
           duration]
  (g/spawn-effect! ctx
                   position
                   {:entity/alert-friendlies-after-duration
                    {:counter (timer/create elapsed-time duration)
                     :faction faction}}))
