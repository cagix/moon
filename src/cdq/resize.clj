(ns cdq.resize
  (:require [gdl.application]))

(defn do! [ctx width height]
  #_(ctx-schema/validate ctx)
  (gdl.application/update-viewports! ctx))
