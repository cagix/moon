(ns clojure.gdx.graphics.g2d.bitmap-font
  (:require [com.badlogic.gdx.graphics.g2d.bitmap-font :as bitmap-font]
            [com.badlogic.gdx.graphics.g2d.bitmap-font.data :as data]
            [com.badlogic.gdx.utils.align :as align]
            [clojure.string :as str]))

(defn draw! [font batch {:keys [scale text x y up? h-align target-width wrap?]}]
  {:pre [(or (nil? h-align)
             (contains? align/k->value h-align))]}
  (let [text-height (-> text
                        (str/split #"\n")
                        count
                        (* (bitmap-font/line-height font)))
        old-scale (data/scale-x (bitmap-font/data font))]
    (data/set-scale! (bitmap-font/data font) (* old-scale scale))
    (.draw font
           batch
           text
           (float x)
           (float (+ y (if up? text-height 0)))
           (float target-width)
           (get align/k->value (or h-align :center))
           wrap?)
    (data/set-scale! (bitmap-font/data font) old-scale)))
