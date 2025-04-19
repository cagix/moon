(ns gdl.graphics.text
  (:require [clojure.gdx.interop :as interop]
            [clojure.string :as str])
  (:import (com.badlogic.gdx.graphics.g2d BitmapFont BitmapFont$BitmapFontData)))

(defn- text-height [font text]
  (-> text
      (str/split #"\n")
      count
      (* (BitmapFont/.getLineHeight font))))

(defn draw
  "font, h-align, up? and scale are optional.
  h-align one of: :center, :left, :right. Default :center.
  up? renders the font over y, otherwise under.
  scale will multiply the drawn text size with the scale."
  [{:keys [cdq.context/unit-scale
           gdl.graphics/batch
           gdl.graphics/default-font]}
   {:keys [font x y text h-align up? scale]}]
  {:pre [unit-scale]}
  (let [^BitmapFont font (or font default-font)
        data (.getData font)
        old-scale (float (.scaleX data))
        new-scale (float (* old-scale
                            (float unit-scale)
                            (float (or scale 1))))
        target-width (float 0)
        wrap? false]
    (.setScale data new-scale)
    (.draw font
           batch
           text
           (float x)
           (float (+ y (if up? (text-height font text) 0)))
           target-width
           (interop/k->align (or h-align :center))
           wrap?)
    (.setScale data old-scale)))
