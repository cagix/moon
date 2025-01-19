(ns cdq.render.update-stage
  (:require cdq.ui))

(defn render [context]
  (cdq.ui/act (:cdq.context/stage context) context)
  context)
