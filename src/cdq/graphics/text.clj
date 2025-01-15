(ns cdq.graphics.text
  (:require [clojure.graphics.2d.bitmap-font :as font]
            [clojure.interop :as interop]
            [clojure.string :as str]))

(defn- text-height [font text]
  (-> text
      (str/split #"\n")
      count
      (* (font/line-height font))))

(defn draw
  "font, h-align, up? and scale are optional.
  h-align one of: :center, :left, :right. Default :center.
  up? renders the font over y, otherwise under.
  scale will multiply the drawn text size with the scale."
  [{:keys [clojure.context/unit-scale
           clojure.graphics/batch
           clojure.graphics/default-font]}
   {:keys [font x y text h-align up? scale]}]
  {:pre [unit-scale]}
  (let [font (or font default-font)
        data (font/data font)
        old-scale (float (font/scale-x data))]
    (font/set-scale data (* old-scale
                            (float unit-scale)
                            (float (or scale 1))))
    (font/draw :font font
               :batch batch
               :text text
               :x x
               :y (+ y (if up? (text-height font text) 0))
               :align (interop/k->align (or h-align :center)))
    (font/set-scale data old-scale)))
