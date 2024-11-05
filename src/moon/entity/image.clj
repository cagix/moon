(ns moon.entity.image
  (:require [gdl.graphics.image :as image]))

(defn render [image entity]
  (image/draw-rotated-centered image
                               (or (:rotation-angle entity) 0)
                               (:position entity)))
