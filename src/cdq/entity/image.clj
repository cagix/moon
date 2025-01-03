(ns cdq.entity.image
  (:require [gdl.context :as c]))

(defn render-default [[_ image] entity c]
  (c/draw-rotated-centered c
                           image
                           (or (:rotation-angle entity) 0)
                           (:position entity)))
