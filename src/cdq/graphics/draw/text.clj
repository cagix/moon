(ns cdq.graphics.draw.text
  (:require [clojure.gdx.utils.align :as align]
            [clojure.string :as str])
  (:import (com.badlogic.gdx.graphics.g2d BitmapFont)))

(defn- text-height [^BitmapFont font text]
  (-> text
      (str/split #"\n")
      count
      (* (.getLineHeight font))))

(defn- draw! [^BitmapFont font batch {:keys [scale text x y up? h-align target-width wrap?]}]
  {:pre [(or (nil? h-align)
             (contains? align/k->value h-align))]}
  (let [old-scale (.scaleX (.getData font))]
    (.setScale (.getData font) (* old-scale scale))
    (.draw font
           batch
           text
           (float x)
           (float (+ y (if up? (text-height font text) 0)))
           (float target-width)
           (get align/k->value (or h-align :center))
           wrap?)
    (.setScale (.getData font) old-scale)))

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
