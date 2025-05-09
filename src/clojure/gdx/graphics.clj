(ns clojure.gdx.graphics
  (:require [clojure.gdx :as gdx]
            [clojure.gdx.interop :as interop]
            [clojure.gdx.math :refer [clamp]]
            [clojure.string :as str])
  (:import (com.badlogic.gdx.files FileHandle)
           (com.badlogic.gdx.graphics Color Pixmap Pixmap$Format Texture Texture$TextureFilter OrthographicCamera)
           (com.badlogic.gdx.graphics.g2d BitmapFont SpriteBatch Batch)
           (com.badlogic.gdx.graphics.g2d.freetype FreeTypeFontGenerator FreeTypeFontGenerator$FreeTypeFontParameter)
           (com.badlogic.gdx.math Vector2)
           (com.badlogic.gdx.utils ScreenUtils)
           (com.badlogic.gdx.utils.viewport Viewport FitViewport)))

(defn clear-screen! []
  (ScreenUtils/clear Color/BLACK))

(defn sprite-batch []
  (SpriteBatch.))

(defn white-pixel-texture []
  (let [pixmap (doto (Pixmap. 1 1 Pixmap$Format/RGBA8888)
                 (.setColor Color/WHITE)
                 (.drawPixel 0 0))
        texture (Texture. pixmap)]
    (.dispose pixmap)
    texture))

(defn pixmap [path]
  (Pixmap. ^FileHandle (gdx/internal path)))

(defn- font-params [{:keys [size]}]
  (let [params (FreeTypeFontGenerator$FreeTypeFontParameter.)]
    (set! (.size params) size)
    ; .color and this:
    ;(set! (.borderWidth parameter) 1)
    ;(set! (.borderColor parameter) red)
    (set! (.minFilter params) Texture$TextureFilter/Linear) ; because scaling to world-units
    (set! (.magFilter params) Texture$TextureFilter/Linear)
    params))

(defn- generate-font [file-handle params]
  (let [generator (FreeTypeFontGenerator. file-handle)
        font (.generateFont generator (font-params params))]
    (.dispose generator)
    font))

(defn truetype-font [{:keys [file size quality-scaling]}]
  (let [^BitmapFont font (generate-font (gdx/internal file)
                                        {:size (* size quality-scaling)})]
    (.setScale (.getData font) (float (/ quality-scaling)))
    (set! (.markupEnabled (.getData font)) true)
    (.setUseIntegerPositions font false) ; otherwise scaling to world-units (/ 1 48)px not visible
    font))

(defn fit-viewport
  ([width height]
   (fit-viewport width height (OrthographicCamera.)))
  ([width height camera]
   (proxy [FitViewport clojure.lang.ILookup] [width height camera]
     (valAt
       ([key]
        (interop/k->viewport-field this key))
       ([key _not-found]
        (interop/k->viewport-field this key))))))

(defn world-viewport [world-unit-scale {:keys [width height]}]
  (let [camera (OrthographicCamera.)
        world-width  (* width world-unit-scale)
        world-height (* height world-unit-scale)
        y-down? false]
    (.setToOrtho camera y-down? world-width world-height)
    (fit-viewport world-width world-height camera)))

; touch coordinates are y-down, while screen coordinates are y-up
; so the clamping of y is reverse, but as black bars are equal it does not matter
(defn unproject-mouse-position
  "Returns vector of [x y]."
  [viewport]
  (let [mouse-x (clamp (gdx/input-x)
                       (:left-gutter-width viewport)
                       (:right-gutter-x    viewport))
        mouse-y (clamp (gdx/input-y)
                       (:top-gutter-height viewport)
                       (:top-gutter-y      viewport))]
    (let [v2 (Viewport/.unproject viewport (Vector2. mouse-x mouse-y))]
      [(.x v2) (.y v2)])))

(defn draw-texture-region [^Batch batch texture-region [x y] [w h] rotation color]
  (if color (.setColor batch color))
  (.draw batch
         texture-region
         x
         y
         (/ (float w) 2) ; rotation origin
         (/ (float h) 2)
         w
         h
         1 ; scale-x
         1 ; scale-y
         rotation)
  (if color (.setColor batch Color/WHITE)))

(defn- text-height [^BitmapFont font text]
  (-> text
      (str/split #"\n")
      count
      (* (.getLineHeight font))))

(defn draw-text! [{:keys [^BitmapFont font
                          scale
                          batch
                          x
                          y
                          text
                          h-align
                          up?]}]
  (let [data (.getData font)
        old-scale (float (.scaleX data))
        new-scale (float (* old-scale (float scale)))
        target-width (float 0)
        wrap? false]
    (.setScale data new-scale)
    (.draw font
           batch
           text
           (float x)
           (float (+ y (if up? (text-height font text) 0)))
           target-width
           (interop/k->align (or h-align :center))
           wrap?)
    (.setScale data old-scale)))
