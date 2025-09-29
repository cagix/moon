(ns gdl.graphics.freetype
  (:require [com.badlogic.gdx.graphics.texture.filter :as texture-filter])
  (:import (com.badlogic.gdx.graphics.g2d BitmapFont)
           (com.badlogic.gdx.graphics.g2d.freetype FreeTypeFontGenerator
                                                   FreeTypeFontGenerator$FreeTypeFontParameter)))

(defn- create-parameter
  [{:keys [size
           min-filter
           mag-filter]}]
  (let [params (FreeTypeFontGenerator$FreeTypeFontParameter.)]
    (set! (.size params) size)
    (set! (.minFilter params) min-filter)
    (set! (.magFilter params) mag-filter)
    params))

(defn- configure!
  [^BitmapFont font
   {:keys [scale
           enable-markup?
           use-integer-positions?]}]
  (.setScale (.getData font) scale)
  (set! (.markupEnabled (.getData font)) enable-markup?)
  (.setUseIntegerPositions font use-integer-positions?)
  font)

(defn generate-font
  [file-handle
   {:keys [size
           quality-scaling
           enable-markup?
           use-integer-positions?]}]
  (let [generator (FreeTypeFontGenerator. file-handle)
        font (.generateFont generator (create-parameter {:size (* size quality-scaling)
                                                         ; :texture-filter/linear because scaling to world-units
                                                         :min-filter (texture-filter/k->value :linear)
                                                         :mag-filter (texture-filter/k->value :linear)}))]
    (configure! font {:scale (/ quality-scaling)
                      :enable-markup? enable-markup?
                      :use-integer-positions? use-integer-positions?})))
