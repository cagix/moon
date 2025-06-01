(ns cdq.game)

(defprotocol Game
  (reset-game-state! [_ world-fn]))
