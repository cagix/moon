(ns cdq.ui.dev-menu.update-labels.zoom
  (:require [cdq.graphics :as graphics]))

(defn create [icon]
  {:label "Zoom"
   :update-fn graphics/zoom-level
   :icon icon})
