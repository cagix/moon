(ns cdq.render.camera-controls
  (:require [clojure.graphics.camera :as camera]
            [clojure.input :as input]))

(defn do! [{:keys [ctx/config
                   ctx/input
                   ctx/world-viewport]
            :as ctx}
           {:keys [zoom-speed]}]
  (let [controls (:controls config)
        camera (:camera world-viewport)]
    (when (input/key-pressed? input (:zoom-in controls))  (camera/inc-zoom! camera    zoom-speed))
    (when (input/key-pressed? input (:zoom-out controls)) (camera/inc-zoom! camera (- zoom-speed))))
  ctx)
