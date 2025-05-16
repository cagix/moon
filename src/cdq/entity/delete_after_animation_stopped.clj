(ns cdq.entity.delete-after-animation-stopped
  (:require [cdq.animation :as animation]
            [cdq.entity :as entity]
            [cdq.utils :refer [defcomponent]]))

(defcomponent :entity/delete-after-animation-stopped?
  (entity/create! [_ eid]
    (-> @eid :entity/animation :looping? not assert)
    nil)

  (entity/tick! [_ eid]
    (when (animation/stopped? (:entity/animation @eid))
      [[:tx/mark-destroyed eid]])))

