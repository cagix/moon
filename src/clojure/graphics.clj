(ns clojure.graphics
  (:require [cdq.interop :as interop]
            [clojure.string :as str])
  (:import (com.badlogic.gdx Gdx)
           (com.badlogic.gdx.graphics Color
                                      Pixmap
                                      Pixmap$Format
                                      Texture
                                      Texture$TextureFilter)
           (com.badlogic.gdx.graphics.g2d BitmapFont)
           (com.badlogic.gdx.graphics.g2d.freetype FreeTypeFontGenerator
                                                   FreeTypeFontGenerator$FreeTypeFontParameter)
           (com.badlogic.gdx.utils ScreenUtils)))

(defn color [r g b a]
  (Color. (float r)
          (float g)
          (float b)
          (float a)))

(defn frames-per-second []
  (.getFramesPerSecond Gdx/graphics))

(defn clear-screen! []
  (ScreenUtils/clear Color/BLACK))

(defn white-pixel-texture []
  (let [pixmap (doto (Pixmap. 1 1 Pixmap$Format/RGBA8888)
                 (.setColor Color/WHITE)
                 (.drawPixel 0 0))
        texture (Texture. pixmap)]
    (.dispose pixmap)
    texture))

(defn cursor [path hotspot-x hotspot-y]
  (let [pixmap (Pixmap. (.internal Gdx/files path))
        cursor (.newCursor Gdx/graphics pixmap hotspot-x hotspot-y)]
    (.dispose pixmap)
    cursor))

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
  (let [^BitmapFont font (generate-font (.internal Gdx/files file)
                                        {:size (* size quality-scaling)})]
    (.setScale (.getData font) (float (/ quality-scaling)))
    (set! (.markupEnabled (.getData font)) true)
    (.setUseIntegerPositions font false) ; otherwise scaling to world-units not visible
    font))

(defn- text-height [^BitmapFont font text]
  (-> text
      (str/split #"\n")
      count
      (* (.getLineHeight font))))

(defn draw-text! [^BitmapFont font batch {:keys [scale x y text h-align up?]}]
  (let [data (.getData font)
        old-scale (float (.scaleX data))
        new-scale (float (* old-scale (float scale)))
        target-width (float 0)
        wrap? false]
    (.setScale data new-scale)
    (.draw font
           (:java-object batch)
           text
           (float x)
           (float (+ y (if up? (text-height font text) 0)))
           target-width
           (interop/k->align (or h-align :center))
           wrap?)
    (.setScale data old-scale)))
