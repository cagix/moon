(ns cdq.tx.spawn-alert
  (:require [clojure.ctx.effect-handler :refer [do!]]
            [cdq.timer :as timer]))

(defmethod do! :tx/spawn-alert [[_ position faction duration]
                                {:keys [ctx/elapsed-time] :as ctx}]
  (do! [:tx/spawn-effect
        position
        {:entity/alert-friendlies-after-duration
         {:counter (timer/create elapsed-time duration)
          :faction faction}}]
       ctx))
