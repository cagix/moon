(ns forge.entity.delete-after-animation-stopped?
  (:require [anvil.animation :as animation]))

(defn create [_ eid]
  (-> @eid :entity/animation :looping? not assert))

(defn tick [_ eid]
  (when (animation/stopped? (:entity/animation @eid))
    (swap! eid assoc :entity/destroyed? true)))
