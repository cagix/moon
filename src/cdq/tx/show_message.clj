(ns cdq.tx.show-message
  (:require [cdq.ui.message :as message]
            [gdl.ui :as ui]))

(defn do! [{:keys [ctx/stage]} message]
  (-> stage
      ui/root
      (ui/find-actor "player-message")
      (message/show! message)))
