(ns cdq.render.camera-controls
  (:require [cdq.g :as g]))

(defn do! [{:keys [ctx/config]
            :as ctx}]
  (let [controls (:controls config)
        zoom-speed (:zoom-speed config)]
    (when (g/key-pressed? ctx (:zoom-in controls))  (g/inc-zoom! ctx    zoom-speed))
    (when (g/key-pressed? ctx (:zoom-out controls)) (g/inc-zoom! ctx (- zoom-speed))))
  ctx)
