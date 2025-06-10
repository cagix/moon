(ns cdq.render.camera-controls
  (:require [gdl.graphics.camera :as camera]
            [gdl.input :as input]))

(defn do! [{:keys [ctx/config
                   ctx/gdx
                   ctx/graphics]
            :as ctx}
           {:keys [zoom-speed]}]
  (let [controls (:controls config)
        camera (:camera (:world-viewport graphics))]
    (when (input/key-pressed? gdx (:zoom-in controls))  (camera/inc-zoom! camera    zoom-speed))
    (when (input/key-pressed? gdx (:zoom-out controls)) (camera/inc-zoom! camera (- zoom-speed))))
  ctx)
