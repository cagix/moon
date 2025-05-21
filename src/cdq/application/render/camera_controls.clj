(ns cdq.application.render.camera-controls
  (:require [gdl.graphics.camera :as camera]
            [gdl.input :as input]))

(defn do! [{:keys [ctx/controls
                   ctx/world-viewport
                   ctx/zoom-speed]}]
  (when (input/key-pressed? (get controls :zoom-in))
    (camera/inc-zoom! (:camera world-viewport) zoom-speed))
  (when (input/key-pressed? (get controls :zoom-out))
    (camera/inc-zoom! (:camera world-viewport) (- zoom-speed)))
  nil)
