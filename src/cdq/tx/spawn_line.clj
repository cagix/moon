(ns cdq.tx.spawn-line
  (:require [cdq.ctx.effect-handler :refer [do!]]))

(defmethod do! :tx/spawn-line [[_ {:keys [start end duration color thick?]}] ctx]
  (do! [:tx/spawn-effect
        start
        {:entity/line-render {:thick? thick? :end end :color color}
         :entity/delete-after-duration duration}]
       ctx))
