(ns forge.entity.states
  (:require [forge.entity :as entity]
            [forge.entity.state :as state]))

(defmethod entity/->v :npc-dead [[_ eid]]
  {:eid eid})

(defmethod state/enter :npc-dead [[_ {:keys [eid]}]]
  (swap! eid assoc :entity/destroyed? true))

