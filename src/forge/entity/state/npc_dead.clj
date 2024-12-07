(ns forge.entity.state.npc-dead
  (:require [clojure.utils :refer [defmethods]]
            [forge.entity :refer [->v]]
            [forge.entity.state :refer [enter]]))

(defmethods :npc-dead
  (->v  [[_ eid]]
    {:eid eid})

  (enter [[_ {:keys [eid]}]]
    (swap! eid assoc :entity/destroyed? true)))
