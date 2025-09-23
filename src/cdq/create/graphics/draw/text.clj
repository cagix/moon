(ns cdq.create.graphics.draw.text
  (:require [gdl.graphics.bitmap-font :as bitmap-font]))

(defn do!
  [{:keys [graphics/batch
           graphics/unit-scale
           graphics/default-font]}
   {:keys [font scale x y text h-align up?]}]
  (bitmap-font/draw! (or font default-font)
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
