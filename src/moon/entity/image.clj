(ns moon.entity.image
  (:require [gdl.graphics.image :as image]))

(defn render [[_ image] entity]
  (image/draw-rotated-centered image
                               (or (:rotation-angle entity) 0)
                               (:position entity)))
