(ns cdq.game.default-font
  (:require [cdq.ctx :as ctx]
            [cdq.interop :as interop]
            [cdq.font :as font]
            [cdq.utils :as utils]
            [clojure.string :as str])
  (:import (com.badlogic.gdx Gdx)
           (com.badlogic.gdx.graphics Texture$TextureFilter)
           (com.badlogic.gdx.graphics.g2d BitmapFont)
           (com.badlogic.gdx.graphics.g2d.freetype FreeTypeFontGenerator
                                                   FreeTypeFontGenerator$FreeTypeFontParameter)
           (com.badlogic.gdx.utils Disposable)))

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

(defn- truetype-font [{:keys [file size quality-scaling]}]
  (let [^BitmapFont font (generate-font (.internal Gdx/files file)
                                        {:size (* size quality-scaling)})]
    (.setScale (.getData font) (float (/ quality-scaling)))
    (set! (.markupEnabled (.getData font)) true)
    (.setUseIntegerPositions font false) ; otherwise scaling to world-units (/ 1 48)px not visible
    font))

(defn- text-height [^BitmapFont font text]
  (-> text
      (str/split #"\n")
      count
      (* (.getLineHeight font))))

(defn- ->reify [^BitmapFont font]
  (reify
    font/Font
    (draw-text! [_ batch {:keys [scale x y text h-align up?]}]
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

    Disposable
    (dispose [_]
      (.dispose font))))

(defn do! []
  (utils/bind-root #'ctx/default-font (->reify
                                       (truetype-font {:file "fonts/exocet/films.EXL_____.ttf"
                                                       :size 16
                                                       :quality-scaling 2}))))
