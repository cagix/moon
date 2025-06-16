(ns cdq.entity.delete-after-animation-stopped
  (:require [cdq.animation :as animation]))

(defn create! [_ eid _ctx]
  (-> @eid :entity/animation :looping? not assert)
  nil)

(defn tick! [_ eid _world]
  (when (animation/stopped? (:entity/animation @eid))
    [[:tx/mark-destroyed eid]]))
