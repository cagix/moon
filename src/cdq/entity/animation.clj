(ns cdq.entity.animation
  (:require [cdq.animation :as animation]
            [cdq.entity :as entity]
            [gdl.utils :refer [defcomponent]]))

(defcomponent :entity/animation
  (entity/create! [[_ animation] eid _ctx]
    [[:tx/assoc eid :entity/image (animation/current-frame animation)]])

  (entity/tick! [[_ animation] eid _ctx]
    [[:tx/update-animation eid animation]]))
