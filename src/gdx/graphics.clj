(ns gdx.graphics
  (:require [gdx.graphics.color :as color])
  (:import (clojure.lang ILookup)
           (com.badlogic.gdx Graphics)
           (com.badlogic.gdx.files FileHandle)
           (com.badlogic.gdx.graphics OrthographicCamera
                                      Pixmap
                                      Pixmap$Format
                                      Texture)
           (com.badlogic.gdx.utils.viewport FitViewport
                                            Viewport)))

(defn delta-time [^Graphics graphics]
  (.getDeltaTime graphics))

(defn frames-per-second [^Graphics graphics]
  (.getFramesPerSecond graphics))

(defn create-cursor [^Graphics graphics ^FileHandle file-handle [hotspot-x hotspot-y]]
  (let [pixmap (Pixmap. file-handle)
        cursor (.newCursor graphics pixmap hotspot-x hotspot-y)]
    (.dispose pixmap)
    cursor))

(defn set-cursor! [^Graphics graphics cursor]
  (.setCursor graphics cursor))

(defn white-pixel-texture []
  (let [pixmap (doto (Pixmap. 1 1 Pixmap$Format/RGBA8888)
                 (.setColor (color/->obj :white))
                 (.drawPixel 0 0))
        texture (Texture. pixmap)]
    (.dispose pixmap)
    texture))

(defn load-texture [file-handle]
  (Texture. file-handle))

(defn orthographic-camera
  ([]
   (OrthographicCamera.))
  ([& {:keys [y-down? world-width world-height]}]
   (doto (OrthographicCamera.)
     (.setToOrtho y-down?
                  world-width
                  world-height))))

(defn fit-viewport [width height camera]
  (proxy [FitViewport ILookup] [width height camera]
    (valAt [k]
      (case k
        :width  (Viewport/.getWorldWidth  this)
        :height (Viewport/.getWorldHeight this)
        :camera (Viewport/.getCamera      this)))))
