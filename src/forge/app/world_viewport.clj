(ns ^:no-doc forge.app.world-viewport
  (:require [forge.core :refer :all])
  (:import (com.badlogic.gdx.graphics OrthographicCamera)
           (com.badlogic.gdx.utils.viewport FitViewport)))

(defmethods :app/world-viewport
  (app-create [[_ [width height tile-size]]]
    (bind-root #'world-unit-scale (float (/ tile-size)))
    (bind-root #'world-viewport-width  width)
    (bind-root #'world-viewport-height height)
    (bind-root #'world-viewport (let [world-width  (* width  world-unit-scale)
                                      world-height (* height world-unit-scale)
                                      camera (OrthographicCamera.)
                                      y-down? false]
                                  (.setToOrtho camera y-down? world-width world-height)
                                  (FitViewport. world-width world-height camera))))
  (app-resize [_ w h]
    (.update world-viewport w h true)))
