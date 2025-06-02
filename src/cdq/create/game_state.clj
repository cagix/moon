(ns cdq.create.game-state
  (:require [cdq.ctx :as ctx]))

(defn do! [{:keys [ctx/config] :as ctx}]
  (ctx/reset-game-state! ctx (:world-fn config)))
