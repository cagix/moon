(ns cdq.graphics.draw.text
  (:require [clojure.gdx.graphics.g2d.bitmap-font :as bitmap-font]
            [clojure.gdx.graphics.g2d.bitmap-font.data :as data]
            [clojure.gdx.utils.align :as align]
            [clojure.string :as str]))

(defn- text-height [font text]
  (-> text
      (str/split #"\n")
      count
      (* (bitmap-font/line-height font))))

(defn- draw! [font batch {:keys [scale text x y up? h-align target-width wrap?]}]
  {:pre [(or (nil? h-align)
             (contains? align/k->value h-align))]}
  (let [old-scale (data/scale-x (bitmap-font/data font))]
    (data/set-scale! (bitmap-font/data font) (* old-scale scale))
    (.draw font
           batch
           text
           (float x)
           (float (+ y (if up? (text-height font text) 0)))
           (float target-width)
           (get align/k->value (or h-align :center))
           wrap?)
    (data/set-scale! (bitmap-font/data font) old-scale)))

(defn do!
  [{:keys [graphics/batch
           graphics/unit-scale
           graphics/default-font]}
   {:keys [font scale x y text h-align up?]}]
  (draw! (or font default-font)
         batch
         {:scale (* (float @unit-scale)
                    (float (or scale 1)))
          :text text
          :x x
          :y y
          :up? up?
          :h-align h-align
          :target-width 0
          :wrap? false}))
