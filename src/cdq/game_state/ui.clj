(ns cdq.game-state.ui
  (:require [gdl.ui.stage :as stage]))

(defn reset-stage! [{:keys [ctx/config
                            ctx/stage]
                     :as ctx}]
  (stage/clear! stage)
  (doseq [[create-actor params] (:cdq.ctx.game/ui-actors config)]
    (stage/add! stage (create-actor ctx params)))
  ctx)
