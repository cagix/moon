(ns cdq.ctx.render.window-camera-controls
  (:require [cdq.graphics.camera :as camera]
            [cdq.input :as input]
            [cdq.ui :as ui]))

(def zoom-speed 0.025)

(defn do!
  [{:keys [ctx/graphics
           ctx/input
           ctx/stage]
    :as ctx}]
  (when (input/zoom-in?            input) (camera/change-zoom! graphics zoom-speed))
  (when (input/zoom-out?           input) (camera/change-zoom! graphics (- zoom-speed)))
  (when (input/close-windows?      input) (ui/close-all-windows!         stage))
  (when (input/toggle-inventory?   input) (ui/toggle-inventory-visible!  stage))
  (when (input/toggle-entity-info? input) (ui/toggle-entity-info-window! stage))
  ctx)
