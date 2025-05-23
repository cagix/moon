(ns cdq.tx.spawn-alert
  (:require [cdq.g :as g]))

(defn do! [ctx position faction duration]
  (g/spawn-effect! ctx
                   position
                   {:entity/alert-friendlies-after-duration
                    {:counter (g/create-timer ctx duration)
                     :faction faction}}))
