(ns cdq.application.render.window-and-camera-controls
  (:require [cdq.input :as input]
            [cdq.graphics :as graphics]
            [cdq.stage :as stage]))

(let [zoom-speed 0.025]
  (defn do!
    [{:keys [ctx/graphics
             ctx/input
             ctx/stage]
      :as ctx}]
    (when (input/zoom-in?            input) (graphics/change-zoom! graphics zoom-speed))
    (when (input/zoom-out?           input) (graphics/change-zoom! graphics (- zoom-speed)))
    (when (input/close-windows?      input) (stage/close-all-windows!         stage))
    (when (input/toggle-inventory?   input) (stage/toggle-inventory-visible!  stage))
    (when (input/toggle-entity-info? input) (stage/toggle-entity-info-window! stage))
    ctx))
