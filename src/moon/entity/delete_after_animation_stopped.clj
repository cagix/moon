(ns moon.entity.delete-after-animation-stopped
  (:require [moon.animation :as animation]
            [moon.entity :as entity]))

(defc :entity/delete-after-animation-stopped?
  (entity/create [_ eid]
    (-> @eid :entity/animation :looping? not assert))

  (entity/tick [_ eid]
    (when (animation/stopped? (:entity/animation @eid))
      [[:e/destroy eid]])))
