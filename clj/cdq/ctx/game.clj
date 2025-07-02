(ns cdq.ctx.game
  (:require [master.yoda :as yoda]))

(defn reset-game-state! [{:keys [ctx/config]
                          :as ctx}]
  (reduce yoda/render* ctx (:config/game-state-pipeline config)))
