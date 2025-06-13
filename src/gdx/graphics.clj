(ns gdx.graphics
  (:require [gdx.graphics.color :as color])
  (:import (clojure.lang ILookup)
           (com.badlogic.gdx Graphics)
           (com.badlogic.gdx.files FileHandle)
           (com.badlogic.gdx.graphics OrthographicCamera
                                      Pixmap
                                      Pixmap$Format
                                      Texture)
           (com.badlogic.gdx.graphics.g2d Batch)
           (com.badlogic.gdx.math Vector2)
           (com.badlogic.gdx.utils.viewport FitViewport
                                            Viewport)))

(defn delta-time [^Graphics graphics]
  (.getDeltaTime graphics))

(defn frames-per-second [^Graphics graphics]
  (.getFramesPerSecond graphics))

(defn pixmap ^Pixmap [^FileHandle file-handle]
  (Pixmap. file-handle))

(defn new-cursor [^Graphics graphics pixmap hotspot-x hotspot-y]
  (.newCursor graphics pixmap hotspot-x hotspot-y))

(defn set-cursor! [^Graphics graphics cursor]
  (.setCursor graphics cursor))

(defn white-pixel-texture []
  (let [pixmap (doto (Pixmap. 1 1 Pixmap$Format/RGBA8888)
                 (.setColor (color/->obj :white))
                 (.drawPixel 0 0))
        texture (Texture. pixmap)]
    (.dispose pixmap)
    texture))

(defn load-texture [^String path]
  (Texture. path))

(defn orthographic-camera
  ([]
   (OrthographicCamera.))
  ([& {:keys [y-down? world-width world-height]}]
   (doto (OrthographicCamera.)
     (.setToOrtho y-down?
                  world-width
                  world-height))))

(defn draw-texture-region! [^Batch batch texture-region [x y] [w h] rotation]
  (.draw batch
         texture-region
         x
         y
         (/ (float w) 2) ; origin-x
         (/ (float h) 2) ; origin-y
         w
         h
         1 ; scale-x
         1 ; scale-y
         rotation))

(defn fit-viewport [width height camera]
  (proxy [FitViewport ILookup] [width height camera]
    (valAt [k]
      (case k
        :width  (Viewport/.getWorldWidth  this)
        :height (Viewport/.getWorldHeight this)
        :camera (Viewport/.getCamera      this)))))

(defn- clamp [value min max]
  (cond
   (< value min) min
   (> value max) max
   :else value))

; touch coordinates are y-down, while screen coordinates are y-up
; so the clamping of y is reverse, but as black bars are equal it does not matter
; TODO clamping only works for gui-viewport ?
; TODO ? "Can be negative coordinates, undefined cells."
(defn unproject [^Viewport viewport [x y]]
  (let [x (clamp x (.getLeftGutterWidth viewport) (.getRightGutterX    viewport))
        y (clamp y (.getTopGutterHeight viewport) (.getTopGutterY      viewport))]
    (let [vector2 (.unproject viewport (Vector2. x y))]
      [(.x vector2)
       (.y vector2)])))

(defn draw-on-viewport! [^Batch batch viewport f]
  ; fix scene2d.ui.tooltip flickering ( maybe because I dont call super at act Actor which is required ...)
  ; -> also Widgets, etc. ? check.
  (.setColor batch (color/->obj :white))
  (.setProjectionMatrix batch (.combined (Viewport/.getCamera viewport)))
  (.begin batch)
  (f)
  (.end batch))
