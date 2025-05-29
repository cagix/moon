(ns cdq.render.camera-controls
  (:require [gdl.graphics :as graphics]
            [gdl.input :as input]))

(defn do! [{:keys [ctx/config
                   ctx/graphics
                   ctx/input]
            :as ctx}]
  (let [controls (:controls config)
        zoom-speed (:zoom-speed config)]
    (when (input/key-pressed? input (:zoom-in controls))  (graphics/inc-zoom! graphics    zoom-speed))
    (when (input/key-pressed? input (:zoom-out controls)) (graphics/inc-zoom! graphics (- zoom-speed))))
  ctx)
