(ns cdq.ctx.game
  (:require [cdq.core :as core]))

(defn reset-game-state! [{:keys [ctx/config]
                          :as ctx}]
  (reduce core/render* ctx (:config/game-state-pipeline config)))
