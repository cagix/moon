(ns cdq.tx.show-modal
  (:require [cdq.ctx.effect-handler :refer [do!]]))

(defmethod do! :tx/show-modal [[_ opts] _ctx]
  [:world.event/show-modal-window opts])
