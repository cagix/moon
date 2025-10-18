(ns cdq.game.render.render-stage
  (:import (cdq.ui Stage)))

(defn step
  [{:keys [^Stage ctx/stage]
    :as ctx}]
  (set! (.ctx stage) ctx)
  (.act  stage)
  (.draw stage)
  (.ctx  stage))
