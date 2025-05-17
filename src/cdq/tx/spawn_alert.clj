(ns cdq.tx.spawn-alert
  (:require [cdq.ctx :as ctx]
            [cdq.timer :as timer]
            [cdq.tx.spawn-entity]))

(defn do! [position faction duration]
  (cdq.tx.spawn-entity/do! position
                           ctx/effect-body-props
                           {:entity/alert-friendlies-after-duration
                            {:counter (timer/create ctx/elapsed-time duration)
                             :faction faction}}))
