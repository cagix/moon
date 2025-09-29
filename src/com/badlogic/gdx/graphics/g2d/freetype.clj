(ns com.badlogic.gdx.graphics.g2d.freetype
  (:require [com.badlogic.gdx.graphics.texture.filter :as texture-filter]
            [com.badlogic.gdx.graphics.g2d.bitmap-font :as font]
            [com.badlogic.gdx.graphics.g2d.freetype.generator :as generator]
            [com.badlogic.gdx.graphics.g2d.freetype.parameter :as parameter]))

(defn generate-font
  [file-handle
   {:keys [size
           quality-scaling
           enable-markup?
           use-integer-positions?
           min-filter
           mag-filter]}]
  (let [generator (generator/create file-handle)
        font (generator/generate-font generator
                                      (parameter/create {:size (* size quality-scaling)
                                                         :min-filter (texture-filter/k->value min-filter)
                                                         :mag-filter (texture-filter/k->value mag-filter)}))]
    (font/configure! font {:scale (/ quality-scaling)
                           :enable-markup? enable-markup?
                           :use-integer-positions? use-integer-positions?})))
