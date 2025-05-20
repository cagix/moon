(ns cdq.entity.image
  (:require [cdq.draw :as draw]
            [cdq.entity :as entity]
            [cdq.utils :refer [defcomponent]]))

(defcomponent :entity/image
  (entity/render-default! [[_ image] entity draw]
    (draw/rotated-centered draw
                           image
                           (or (:rotation-angle entity) 0)
                           (:position entity))))
