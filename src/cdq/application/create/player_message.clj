(ns cdq.application.create.player-message)

(defn create [_context]
  (atom {:duration-seconds (:duration-seconds 1.5)}))
