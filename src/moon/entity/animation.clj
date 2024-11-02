(ns moon.entity.animation
  (:require [moon.animation :as animation]
            [moon.entity :as entity]
            [moon.world.time :as time]))

(defn- tx-assoc-image-current-frame [eid animation]
  [:e/assoc eid :entity/image (animation/current-frame animation)])

(defmethods :entity/animation
  {:let animation}
  (entity/create [_ eid]
    [(tx-assoc-image-current-frame eid animation)])

  (entity/tick [[k _] eid]
    [(tx-assoc-image-current-frame eid animation)
     [:e/assoc eid k (animation/tick animation time/delta)]]))
