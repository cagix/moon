(ns cdq.draw.text
  (:require [clojure.gdx.graphics.g2d.bitmap-font :as bitmap-font]))

(defn do!
  [[_ {:keys [font scale x y text h-align up?]}]
   {:keys [ctx/batch
           ctx/unit-scale
           ctx/default-font]}]
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
