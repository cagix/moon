(ns cdq.application.render.try-fetch-state-ctx
  (:require [gdl.scene2d.stage :as stage]))

(defn do!
  [{:keys [ctx/stage]
    :as ctx}]
  (if-let [new-ctx (stage/get-ctx stage)]
    new-ctx
    ctx)) ; first render stage doesnt have context
