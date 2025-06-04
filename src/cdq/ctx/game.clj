(ns cdq.ctx.game
  (:require [cdq.world :as world]))

(defn reset-game-state! [{:keys [ctx/config] :as ctx} world-fn]
  (let [ctx (reduce (fn [ctx f]
                      (f ctx))
                    ctx
                    (:game-state-fns config))]
    (world/create ctx config world-fn)))
