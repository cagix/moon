(ns cdq.draw.centered
  (:require [cdq.graphics :as graphics]))

(defn draw!
  [[_ image position]
   graphics]
  (graphics/draw! [:draw/rotated-centered image 0 position] graphics))
