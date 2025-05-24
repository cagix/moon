(ns cdq.entity.image
  (:require [cdq.entity :as entity]
            [cdq.utils :refer [defcomponent]]))

(defcomponent :entity/image
  (entity/render-default! [[_ image] entity _ctx]
    [[:draw/rotated-centered
      image
      (or (:rotation-angle entity) 0)
      (entity/position entity)]]))
