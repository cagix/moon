(ns cdq.draw.text
  (:require [clojure.string :as str])
  (:import (com.badlogic.gdx.graphics.g2d BitmapFont)
           (com.badlogic.gdx.utils Align)))

(def ^:private align-k->value
  {:bottom       Align/bottom
   :bottom-left  Align/bottomLeft
   :bottom-right Align/bottomRight
   :center       Align/center
   :left         Align/left
   :right        Align/right
   :top          Align/top
   :top-left     Align/topLeft
   :top-right    Align/topRight})

(defn- text-height [^BitmapFont font text]
  (-> text
      (str/split #"\n")
      count
      (* (.getLineHeight font))))

(defn- bitmap-font-draw!
  "font, h-align, up? and scale are optional.
  h-align one of: :center, :left, :right. Default :center.
  up? renders the font over y, otherwise under.
  scale will multiply the drawn text size with the scale."
  [^BitmapFont font
   batch
   {:keys [scale text x y up? h-align target-width wrap?]}]
  {:pre [(or (nil? h-align)
             (contains? align-k->value h-align))]}
  (let [old-scale (.scaleX (.getData font))]
    (.setScale (.getData font) (float (* old-scale scale)))
    (.draw font
           batch
           text
           (float x)
           (float (+ y (if up? (text-height font text) 0)))
           (float target-width)
           (get align-k->value (or h-align :center))
           wrap?)
    (.setScale (.getData font) (float old-scale))))

(defn draw!
  [[_ {:keys [font scale x y text h-align up?]}]
   {:keys [batch
           unit-scale
           default-font]}]
  (bitmap-font-draw! (or font default-font)
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
