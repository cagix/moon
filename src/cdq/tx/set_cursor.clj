(ns cdq.tx.set-cursor
  (:require [cdq.ctx.effect-handler :refer [do!]]))

(defmethod do! :tx/set-cursor [[_ cursor-key]
                               _ctx]
  [:world.event/set-cursor cursor-key])
