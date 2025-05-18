(ns cdq.tx.show-message
  (:require [cdq.ctx :as ctx]
            [cdq.ui.message :as message]
            [gdl.ui :as ui]
            [gdl.ui.stage :as stage]))

(defn do! [message]
  (-> ctx/stage
      stage/root
      (ui/find-actor "player-message")
      (message/show! message)))
