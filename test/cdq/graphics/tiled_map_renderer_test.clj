(ns cdq.graphics.tiled-map-renderer-test
  (:require [clojure.gdx.backends.lwjgl :as lwjgl]
            [clojure.gdx.graphics.camera :as camera]
            [clojure.gdx.tiled :as tiled])
  (:import (com.badlogic.gdx ApplicationAdapter)
           (com.badlogic.gdx.graphics Color OrthographicCamera)
           (com.badlogic.gdx.graphics.g2d SpriteBatch)
           (com.badlogic.gdx.utils Disposable)))

(def screen-width 800)
(def screen-height 600)

(def world-unit-scale (float (/ 48)))

(def tiled-map-path "maps/vampire.tmx")

(def camera-position [32 71])

(defn- color-setter [_color _x _y]
  Color/WHITE)

(defn -main []
  (lwjgl/application! {:title "Test"
                       :windowed-mode {:width screen-width :height screen-height}
                       :foreground-fps 60
                       :dock-icon "moon.png"}
                      (proxy [ApplicationAdapter] []
                        (create []
                          (def tiled-map (tiled/load-map tiled-map-path))
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
