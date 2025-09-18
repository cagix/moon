(ns cdq.application.resize
  (:require [cdq.graphics :as graphics]))

(defn do! [state width height]
  (graphics/update-viewports! (:ctx/graphics @state) width height))
