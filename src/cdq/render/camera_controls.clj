(ns cdq.render.camera-controls
  (:require [gdl.graphics.camera :as camera]
            [gdl.input :as input]))

(defn do! [{:keys [ctx/config
                   ctx/input
                   ctx/graphics]
            :as ctx}
           {:keys [zoom-speed]}]
  (let [controls (:controls config)
        camera (:viewport/camera (:world-viewport graphics))]
    (when (input/key-pressed? input (:zoom-in controls))  (camera/inc-zoom! camera    zoom-speed))
    (when (input/key-pressed? input (:zoom-out controls)) (camera/inc-zoom! camera (- zoom-speed))))
  ctx)
