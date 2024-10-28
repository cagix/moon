(ns moon.entity.animation
  (:require [moon.animation :as animation]
            [moon.component :refer [defc]]
            [moon.entity :as entity]
            [moon.world :as world]))

(defn- tx-assoc-image-current-frame [eid animation]
  [:e/assoc eid :entity/image (animation/current-frame animation)])

(defc :entity/animation
  {:schema :s/animation
   :let animation}
  (entity/create [_ eid]
    [(tx-assoc-image-current-frame eid animation)])

  (entity/tick [[k _] eid]
    [(tx-assoc-image-current-frame eid animation)
     [:e/assoc eid k (animation/tick animation world/delta-time)]]))
