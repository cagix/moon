(ns moon.entity.image
  (:require [moon.entity :as entity]
            [moon.graphics.image :as image]))

(defc :entity/image
  {:schema :s/image
   :let image}
  (entity/render [_ entity]
    (image/draw-rotated-centered image
                                 (or (:rotation-angle entity) 0)
                                 (:position entity))))
