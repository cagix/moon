(ns cdq.application.resize
  (:require [cdq.graphics :as graphics]))

(defn do! [{:keys [ctx/graphics]} width height]
  (graphics/update-viewports! graphics width height))
