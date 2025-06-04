(ns cdq.ctx.game
  (:require [cdq.ui.stage :as stage]
            [cdq.world :as world]))

(defn reset-game-state! [{:keys [ctx/config
                                 ctx/stage]
                          :as ctx}
                         world-fn]
  (stage/clear! stage)
  (doseq [create-actor (:cdq.ctx.game/ui-actors config)]
    (stage/add! stage (create-actor ctx)))
  (world/create ctx (:cdq.ctx.game/world config) world-fn))
