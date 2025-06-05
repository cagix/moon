(ns cdq.render.camera-controls
  (:require [clojure.graphics.camera :as camera]
            [clojure.x :as x]))

(defn do! [{:keys [ctx/config
                   ctx/world-viewport]
            :as ctx}
           {:keys [zoom-speed]}]
  (let [controls (:controls config)
        camera (:camera world-viewport)]
    (when (x/key-pressed? ctx (:zoom-in controls))  (camera/inc-zoom! camera    zoom-speed))
    (when (x/key-pressed? ctx (:zoom-out controls)) (camera/inc-zoom! camera (- zoom-speed))))
  ctx)
