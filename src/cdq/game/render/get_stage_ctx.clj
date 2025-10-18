(ns cdq.game.render.get-stage-ctx
  (:require [cdq.ui :as ui]))

(defn step
  [{:keys [ctx/stage]
    :as ctx}]
  (or (ui/get-ctx stage)
      ctx)) ; first render stage does not have ctx set.
