(ns cdq.tx.show-message
  (:require [cdq.ctx.effect-handler :refer [do!]]
            [cdq.ui.message]
            [gdl.ui.stage :as stage]))

(defmethod do! :tx/show-message [[_ message]
                                 {:keys [ctx/stage]}]
  (-> stage
      (stage/find-actor "player-message")
      (cdq.ui.message/show! message))
  nil)
