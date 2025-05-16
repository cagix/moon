(ns cdq.tx.spawn-alert
  (:require [cdq.ctx :as ctx]
            [cdq.timer :as timer]
            [cdq.impl.world :as world]))

(defn do! [position faction duration]
  (world/spawn-entity! position
                       ctx/effect-body-props
                       {:entity/alert-friendlies-after-duration
                        {:counter (timer/create ctx/elapsed-time duration)
                         :faction faction}}))
