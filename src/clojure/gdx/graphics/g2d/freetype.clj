(ns clojure.gdx.graphics.g2d.freetype
  (:require [clojure.gdx :as gdx]
            [clojure.gdx.graphics.texture :as texture])
  (:import (com.badlogic.gdx.graphics.g2d.freetype FreeTypeFontGenerator
                                                   FreeTypeFontGenerator$FreeTypeFontParameter)))

(defn- ttf-params [size quality-scaling]
  (let [params (FreeTypeFontGenerator$FreeTypeFontParameter.)]
    (set! (.size params) (* size quality-scaling))
    ; .color and this:
    ;(set! (.borderWidth parameter) 1)
    ;(set! (.borderColor parameter) red)
    (set! (.minFilter params) texture/filter-linear) ; because scaling to world-units
    (set! (.magFilter params) texture/filter-linear)
    params))

(defn generate-font [{:keys [file size quality-scaling]}]
  (let [generator (FreeTypeFontGenerator. (gdx/internal-file file))
        font (.generateFont generator (ttf-params size quality-scaling))]
    (.dispose generator)
    (.setScale (.getData font) (float (/ quality-scaling)))
    (set! (.markupEnabled (.getData font)) true)
    (.setUseIntegerPositions font false) ; otherwise scaling to world-units (/ 1 48)px not visible
    font))
