(ns cdq.ui.dev-menu.update-labels.zoom)

(defn create [icon]
  {:label "Zoom"
   :update-fn (comp :camera/zoom :viewport/camera :ctx/world-viewport)
   :icon icon})
