(ns cdq.ctx.get-stage-ctx
  (:require [com.badlogic.gdx.scenes.scene2d.stage :as stage]))

(defn do!
  [{:keys [ctx/stage]
    :as ctx}]
  (if-let [new-ctx (stage/get-ctx stage)]
    new-ctx
    ctx)) ; first render stage doesnt have context
