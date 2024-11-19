(ns moon.entity.image
  (:require [moon.core :refer [draw-rotated-centered]]))

(defn render [image entity]
  (draw-rotated-centered image
                         (or (:rotation-angle entity) 0)
                         (:position entity)))
