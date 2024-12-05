(ns forge.app.world-viewport
  (:require [forge.core :refer [bind-root
                                world-unit-scale
                                world-viewport-width
                                world-viewport-height
                                world-viewport]])
  (:import (com.badlogic.gdx.graphics OrthographicCamera)
           (com.badlogic.gdx.utils.viewport FitViewport)))

(defn create [[_ [width height tile-size]]]
  (bind-root world-unit-scale (float (/ tile-size)))
  (bind-root world-viewport-width  width)
  (bind-root world-viewport-height height)
  (bind-root world-viewport (let [world-width  (* width  world-unit-scale)
                                  world-height (* height world-unit-scale)
                                  camera (OrthographicCamera.)
                                  y-down? false]
                              (.setToOrtho camera y-down? world-width world-height)
                              (FitViewport. world-width world-height camera))))
(defn resize [_ w h]
  (.update world-viewport w h true))
