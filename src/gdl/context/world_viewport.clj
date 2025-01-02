(ns gdl.context.world-viewport
  (:require [clojure.gdx :as gdx]
            [clojure.gdx.graphics.camera :as camera]))

(defn create [[_ {:keys [width height]}] {:keys [gdl.context/world-unit-scale]}]
  (assert world-unit-scale)
  (let [camera (gdx/orthographic-camera)
        world-width  (* width  world-unit-scale)
        world-height (* height world-unit-scale)]
    (camera/set-to-ortho camera world-width world-height :y-down? false)
    (gdx/fit-viewport world-width world-height camera)))

(defn resize [[_ viewport] w h]
  (gdx/resize viewport w h :center-camera? false))
