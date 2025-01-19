(ns cdq.create.player-message)

(defn create [_context]
  (atom {:duration-seconds 1.5}))
