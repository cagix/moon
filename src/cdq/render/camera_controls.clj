(ns cdq.render.camera-controls
  (:require [cdq.input :as input]
            [cdq.graphics.camera :as camera]))

(def ^:private zoom-speed 0.025)

(defn do! [{:keys [ctx/config
                   ctx/input
                   ctx/graphics]
            :as ctx}]
  (let [controls (:controls config)
        camera (:viewport/camera (:world-viewport graphics))]
    (when (input/key-pressed? input (:zoom-in controls))  (camera/inc-zoom! camera    zoom-speed))
    (when (input/key-pressed? input (:zoom-out controls)) (camera/inc-zoom! camera (- zoom-speed))))
  ctx)
