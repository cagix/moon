#_(ns cdq.graphics.tiled-map-renderer-test
  (:require [gdl.tiled :as tiled]
            [gdl.graphics.camera :as camera])
  (:import (com.badlogic.gdx ApplicationAdapter)
           (com.badlogic.gdx.backends.lwjgl3 Lwjgl3Application)
           (com.badlogic.gdx.graphics Color OrthographicCamera)
           (com.badlogic.gdx.graphics.g2d SpriteBatch)
           (com.badlogic.gdx.utils Disposable)
           (org.lwjgl.system Configuration)))

(def screen-width 800)
(def screen-height 600)

(def world-unit-scale (float (/ 48)))

(def tiled-map-path "maps/vampire.tmx")

(def camera-position [32 71])

#_(defn- color-setter [_color _x _y]
  Color/WHITE)

#_(defn -main []
  (.set Configuration/GLFW_LIBRARY_NAME "glfw_async")
  (Lwjgl3Application.
   (proxy [ApplicationAdapter] []
     (create []
       (def tiled-map (tiled/load-tmx-map tiled-map-path))
       (def batch (SpriteBatch.))
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
