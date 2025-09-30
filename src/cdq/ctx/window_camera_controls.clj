(ns cdq.ctx.window-camera-controls
  (:require [cdq.input :as input]
            [cdq.graphics :as graphics]))

(def zoom-speed 0.025)

(defn do!
  [{:keys [ctx/graphics
           ctx/input
           ctx/stage]
    :as ctx}]
  (when (input/zoom-in?            input) (graphics/change-zoom! graphics zoom-speed))
  (when (input/zoom-out?           input) (graphics/change-zoom! graphics (- zoom-speed)))
  (when (input/close-windows?      input) (cdq.stage/close-all-windows!         stage))
  (when (input/toggle-inventory?   input) (cdq.stage/toggle-inventory-visible!  stage))
  (when (input/toggle-entity-info? input) (cdq.stage/toggle-entity-info-window! stage))
  ctx)
