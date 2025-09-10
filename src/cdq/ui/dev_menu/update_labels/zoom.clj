(ns cdq.ui.dev-menu.update-labels.zoom
  (:require [cdq.gdx.graphics :as graphics]))

(defn create [icon]
  {:label "Zoom"
   :update-fn (comp graphics/camera-zoom :ctx/graphics)
   :icon icon})
