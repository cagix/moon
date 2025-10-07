(ns cdq.graphics.create.default-font
  (:require [clojure.gdx.graphics.texture.filter :as texture-filter]
            [clojure.gdx.graphics.g2d.bitmap-font :as bitmap-font]
            [clojure.gdx.graphics.g2d.bitmap-font.data :as data]
            [clojure.gdx.freetype.generator :as generator]
            [clojure.gdx.freetype.parameter :as parameter]))

(defn create [graphics default-font]
  (assoc graphics :graphics/default-font
         (let [file-handle (:file-handle default-font)
               {:keys [size
                       quality-scaling
                       enable-markup?
                       use-integer-positions?
                       min-filter
                       mag-filter]} (:params default-font)]
           (let [generator (generator/create file-handle)
                 font (generator/generate-font generator
                                               (parameter/create {:size (* size quality-scaling)
                                                                  :min-filter (texture-filter/k->value min-filter)
                                                                  :mag-filter (texture-filter/k->value mag-filter)}))]
             (data/set-scale!     (bitmap-font/data font) (/ quality-scaling))
             (data/enable-markup! (bitmap-font/data font) enable-markup?)
             (bitmap-font/set-use-integer-positions! font use-integer-positions?)
             font))))
