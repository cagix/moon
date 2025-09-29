(ns gdl.graphics.g2d.bitmap-font
  (:require [clojure.string :as str]
            [com.badlogic.gdx.utils.align :as align])
  (:import (com.badlogic.gdx.graphics.g2d BitmapFont)))

(defn- text-height [^BitmapFont font text]
  (-> text
      (str/split #"\n")
      count
      (* (.getLineHeight font))))

(defn draw!
  [^BitmapFont font
   batch
   {:keys [scale text x y up? h-align target-width wrap?]}]
  {:pre [(or (nil? h-align)
             (contains? align/k->value h-align))]}
  (let [old-scale (.scaleX (.getData font))]
    (.setScale (.getData font) (float (* old-scale scale)))
    (.draw font
           batch
           text
           (float x)
           (float (+ y (if up? (text-height font text) 0)))
           (float target-width)
           (get align/k->value (or h-align :center))
           wrap?)
    (.setScale (.getData font) (float old-scale))))
