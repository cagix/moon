(ns world.entity.image
  (:require [moon.graphics :as g]
            [moon.component :refer [defc]]
            [world.entity :as entity]))

(defc :entity/image
  {:schema :s/image
   :let image}
  (entity/render [_ entity]
    (g/draw-rotated-centered-image image
                                   (or (:rotation-angle entity) 0)
                                   (:position entity))))
