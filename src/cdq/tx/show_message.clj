(ns cdq.tx.show-message
  (:require [cdq.ctx :as ctx]
            [cdq.ui.message :as message]
            [gdl.ui :as ui]))

(defn do! [message]
  (-> ctx/stage
      ui/root
      (ui/find-actor "player-message")
      (message/show! message)))
