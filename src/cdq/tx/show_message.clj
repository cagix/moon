(ns cdq.tx.show-message
  (:require [cdq.ctx.effect-handler :refer [do!]]))

(defmethod do! :tx/show-message [[_ message] _ctx]
  [:world.event/show-player-message message])
