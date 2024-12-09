(ns forge.entity.image
  (:require [anvil.graphics :refer [draw-rotated-centered]]))

(defn render-default [[_ image] entity]
  (draw-rotated-centered image
                         (or (:rotation-angle entity) 0)
                         (:position entity)))
