(ns cdq.game.when-not-paused
  (:require [cdq.ctx :as ctx]
            [cdq.game :as game]))

(defn do! [transactions]
  (when-not ctx/paused?
    (game/execute! transactions)))
