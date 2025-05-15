(ns cdq.game.camera-controls
  (:require [cdq.ctx :as ctx]
            [clojure.graphics.camera :as camera]
            [clojure.input :as input]))

(defn do! []
  (let [camera (:camera ctx/world-viewport)
        zoom-speed 0.025]
    (when (input/key-pressed? :minus)  (camera/inc-zoom! camera    zoom-speed))
    (when (input/key-pressed? :equals) (camera/inc-zoom! camera (- zoom-speed)))))
