(ns cdq.tx.spawn-alert
  (:require [cdq.timer :as timer]
            [cdq.g :as g]))

(defn do! [{:keys [ctx/effect-body-props
                   ctx/elapsed-time] :as ctx}
           position
           faction
           duration]
  (g/spawn-entity! ctx
                   position
                   effect-body-props
                   {:entity/alert-friendlies-after-duration
                    {:counter (timer/create elapsed-time duration)
                     :faction faction}}))
