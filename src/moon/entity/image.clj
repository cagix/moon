(ns moon.entity.image
  (:require [gdl.graphics.image :as image]
            [moon.entity :as entity]))

(defmethods :entity/image
  {:let image}
  (entity/render [_ entity]
    (image/draw-rotated-centered image
                                 (or (:rotation-angle entity) 0)
                                 (:position entity))))
