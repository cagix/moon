(ns cdq.entity.animation
  (:require [gdl.graphics.animation :as animation]))

(defn create! [[_ animation] eid c]
  (swap! eid assoc :entity/image (animation/current-frame animation)))

(defn tick [[k animation] eid {:keys [cdq.context/delta-time]}]
  (swap! eid #(-> %
                  (assoc :entity/image (animation/current-frame animation))
                  (assoc k (animation/tick animation delta-time)))))
