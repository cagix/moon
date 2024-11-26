(ns ^:no-doc forge.entity.animation
  (:require [forge.entity :as entity]
            [forge.graphics.animation :as animation]
            [moon.world :as world]))

(defn- assoc-image-current-frame [entity animation]
  (assoc entity :entity/image (animation/current-frame animation)))

(defmethod entity/create :entity/animation [[_ animation] eid]
  (swap! eid assoc-image-current-frame animation))

(defmethod entity/tick :entity/animation [[k animation] eid]
  (swap! eid #(-> %
                  (assoc-image-current-frame animation)
                  (assoc k (animation/tick animation world/delta)))))

(defmethod entity/create :entity/delete-after-animation-stopped [_ eid]
  (-> @eid :entity/animation :looping? not assert))

(defmethod entity/tick :entity/delete-after-animation-stopped [_ eid]
  (when (animation/stopped? (:entity/animation @eid))
    (swap! eid assoc :entity/destroyed? true)))
