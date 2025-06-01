(ns cdq.render.camera-controls
  (:require [cdq.graphics :as g]
            [cdq.input :as input]))

(defn do! [{:keys [ctx/config]
            :as ctx}]
  (let [controls (:controls config)
        zoom-speed (:zoom-speed config)]
    (when (input/key-pressed? ctx (:zoom-in controls))  (g/inc-zoom! ctx    zoom-speed))
    (when (input/key-pressed? ctx (:zoom-out controls)) (g/inc-zoom! ctx (- zoom-speed))))
  ctx)
