(ns ^:no-doc moon.entity.image
  (:require [moon.component :refer [defc]]
            [moon.entity :as entity]
            [moon.graphics :as g]))

(defc :entity/image
  {:schema :s/image
   :let image}
  (entity/render [_ entity]
    (g/draw-rotated-centered-image image
                                   (or (:rotation-angle entity) 0)
                                   (:position entity))))
