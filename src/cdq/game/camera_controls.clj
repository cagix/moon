(ns cdq.game.camera-controls
  (:require [cdq.camera :as camera]
            [cdq.ctx :as ctx])
  (:import (com.badlogic.gdx Gdx Input$Keys)))

(defn do! []
  (let [camera (:camera (:world-viewport ctx/graphics))
        zoom-speed 0.025]
    (when (.isKeyPressed Gdx/input Input$Keys/MINUS)  (camera/inc-zoom! camera    zoom-speed))
    (when (.isKeyPressed Gdx/input Input$Keys/EQUALS) (camera/inc-zoom! camera (- zoom-speed)))))
