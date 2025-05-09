(ns clojure.gdx.graphics
  (:require [clojure.gdx :as gdx])
  (:import (com.badlogic.gdx.graphics Texture$TextureFilter)
           (com.badlogic.gdx.graphics.g2d BitmapFont)
           (com.badlogic.gdx.graphics.g2d.freetype FreeTypeFontGenerator FreeTypeFontGenerator$FreeTypeFontParameter)))

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
