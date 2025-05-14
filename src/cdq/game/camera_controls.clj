(ns cdq.game.camera-controls
  (:require [cdq.ctx :as ctx]
            [cdq.graphics.camera :as camera])
  (:import (com.badlogic.gdx Gdx Input$Keys)))

(defn do! []
  (let [camera (:camera (:world-viewport ctx/graphics))
        zoom-speed 0.025]
    (when (.isKeyPressed Gdx/input Input$Keys/MINUS)  (camera/inc-zoom camera    zoom-speed))
    (when (.isKeyPressed Gdx/input Input$Keys/EQUALS) (camera/inc-zoom camera (- zoom-speed)))))
