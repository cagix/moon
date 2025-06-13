(ns cdq.entity.animation
  (:require [cdq.animation :as animation]
            [cdq.entity :as entity]
            [cdq.utils :refer [defmethods]]))

(defn tick! [animation eid {:keys [ctx/delta-time]}]
  [[:tx/assoc eid :entity/image (animation/current-frame animation)]
   [:tx/assoc eid :entity/animation (animation/tick animation delta-time)]])

(defmethods :entity/animation
  (entity/create [[_ v] _ctx]
    (animation/create v))

  (entity/create! [[_ animation] eid _ctx]
    [[:tx/assoc eid :entity/image (animation/current-frame animation)]]))
