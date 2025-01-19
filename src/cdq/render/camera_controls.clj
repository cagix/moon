(ns cdq.render.camera-controls
  (:require [cdq.graphics.camera :as camera]
            [clojure.gdx.input :as input]))

(defn render [{:keys [cdq.graphics/world-viewport]
               :as context}]
  (let [camera (:camera world-viewport)
        zoom-speed 0.025]
    (when (input/key-pressed? :minus)  (camera/inc-zoom camera    zoom-speed))
    (when (input/key-pressed? :equals) (camera/inc-zoom camera (- zoom-speed))))
  context)
