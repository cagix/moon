(ns cdq.entity.delete-after-animation-stopped
  (:require [cdq.entity.animation :as animation]))

(defn tick [_ eid _world]
  (when (animation/stopped? (:entity/animation @eid))
    [[:tx/mark-destroyed eid]]))
