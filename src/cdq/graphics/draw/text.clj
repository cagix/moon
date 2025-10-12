(ns cdq.graphics.draw.text
  (:require [clojure.gdx.graphics.g2d.bitmap-font :as fnt]
            [clojure.gdx.graphics.g2d.bitmap-font.data :as d]
            [clojure.gdx.utils.align :as align]
            [clojure.string :as str]))

(defn- draw! [font batch {:keys [scale text x y up? h-align target-width wrap?]}]
  (let [text-height (fn []
                      (-> text
                          (str/split #"\n")
                          count
                          (* (fnt/line-height font))))
        old-scale (d/scale-x (fnt/data font))]
    (d/set-scale! (fnt/data font) (* old-scale scale))
    (fnt/draw! font
               batch
               text
               x
               (+ y (if up? (text-height) 0))
               target-width
               (or h-align align/center)
               wrap?)
    (d/set-scale! (fnt/data font) old-scale)))

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
