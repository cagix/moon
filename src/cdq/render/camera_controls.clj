(ns cdq.render.camera-controls
  (:require [cdq.g :as g]
            [gdl.input :as input]))

(defn do! [{:keys [ctx/config
                   ctx/input]
            :as ctx}]
  (let [controls (:controls config)
        zoom-speed (:zoom-speed config)]
    (when (input/key-pressed? input (:zoom-in controls))  (g/inc-zoom! ctx    zoom-speed))
    (when (input/key-pressed? input (:zoom-out controls)) (g/inc-zoom! ctx (- zoom-speed))))
  ctx)
