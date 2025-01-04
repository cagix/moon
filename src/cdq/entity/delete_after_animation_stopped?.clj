(ns cdq.entity.delete-after-animation-stopped?
  (:require [gdl.graphics.animation :as animation]))

(defn tick [_ eid c]
  (when (animation/stopped? (:entity/animation @eid))
    (swap! eid assoc :entity/destroyed? true)))
