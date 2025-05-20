(ns cdq.tx.spawn-alert
  (:require [cdq.timer :as timer]
            [cdq.tx.spawn-entity]))

(defn do! [{:keys [ctx/effect-body-props
                   ctx/elapsed-time] :as ctx}
           position
           faction
           duration]
  (cdq.tx.spawn-entity/do! ctx
                           position
                           effect-body-props
                           {:entity/alert-friendlies-after-duration
                            {:counter (timer/create elapsed-time duration)
                             :faction faction}}))
