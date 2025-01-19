(ns cdq.render.draw-stage
  (:require cdq.ui))

(defn render [{:keys [cdq.context/stage] :as context}]
  (cdq.ui/draw stage (assoc context :cdq.context/unit-scale 1))
  context)
