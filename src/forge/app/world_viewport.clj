(ns ^:no-doc forge.app.world-viewport
  (:require [forge.system :as system])
  (:import (com.badlogic.gdx.graphics OrthographicCamera)
           (com.badlogic.gdx.utils.viewport FitViewport)))

(defmethods :app/world-viewport
  (system/create [[_ [width height tile-size]]]
    (bind-root #'system/world-unit-scale (float (/ tile-size)))
    (bind-root #'system/world-viewport-width  width)
    (bind-root #'system/world-viewport-height height)
    (bind-root #'system/world-viewport (let [world-width  (* width  system/world-unit-scale)
                                             world-height (* height system/world-unit-scale)
                                             camera (OrthographicCamera.)
                                             y-down? false]
                                         (.setToOrtho camera y-down? world-width world-height)
                                         (FitViewport. world-width world-height camera))))
  (system/resize [_ w h]
    (.update system/world-viewport w h true)))
