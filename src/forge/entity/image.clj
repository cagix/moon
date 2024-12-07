(ns forge.entity.image
  (:require [forge.entity :refer [render-default]]
            [forge.graphics :refer [draw-rotated-centered]]))

(defmethod render-default :entity/image [[_ image] entity]
  (draw-rotated-centered image
                         (or (:rotation-angle entity) 0)
                         (:position entity)))
