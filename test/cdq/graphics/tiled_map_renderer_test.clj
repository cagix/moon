(ns cdq.graphics.tiled-map-renderer-test
  (:require [clojure.gdx.graphics.camera :as camera]
            [clojure.gdx.graphics.color :as color]
            [clojure.gdx.graphics.g2d.sprite-batch :as sprite-batch]
            [clojure.gdx.tiled :as tiled])
  (:import (com.badlogic.gdx ApplicationAdapter)
           (com.badlogic.gdx.backends.lwjgl3 Lwjgl3Application)
           (com.badlogic.gdx.graphics OrthographicCamera)
           (com.badlogic.gdx.utils Disposable)
           (org.lwjgl.system Configuration)))

(def screen-width 800)
(def screen-height 600)

(def world-unit-scale (float (/ 48)))

(def tiled-map-path "maps/vampire.tmx")

(def camera-position [32 71])

(defn- color-setter [_color _x _y]
  color/white)

(defn -main []
  (.set Configuration/GLFW_LIBRARY_NAME "glfw_async")
  (Lwjgl3Application.
   (proxy [ApplicationAdapter] []
     (create []
       (def tiled-map (tiled/load-map tiled-map-path))
       (def batch (sprite-batch/create))
       (def camera (doto (OrthographicCamera.)
                     (.setToOrtho false ; y-down?
                                  (* screen-width world-unit-scale)
                                  (* screen-height world-unit-scale))))
       (camera/set-position! camera camera-position)
       (def renderer (tiled/renderer tiled-map world-unit-scale batch)))

     (dispose []
       (Disposable/.dispose tiled-map)
       (Disposable/.dispose batch))

     (render []
       (tiled/draw! renderer tiled-map color-setter camera))

     (resize [width height]))))
