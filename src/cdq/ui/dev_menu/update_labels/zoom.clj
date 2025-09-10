(ns cdq.ui.dev-menu.update-labels.zoom
  (:require [cdq.gdx.graphics :as graphics]))

(defn create [icon]
  {:label "Zoom"
   :update-fn graphics/camera-zoom
   :icon icon})
