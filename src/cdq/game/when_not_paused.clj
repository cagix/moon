(ns cdq.game.when-not-paused
  (:require [cdq.ctx :as ctx]
            [cdq.utils :as utils]))

(defn do! [transactions]
  (when-not ctx/paused?
    (utils/execute! transactions)))
