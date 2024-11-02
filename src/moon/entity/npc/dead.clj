(ns moon.entity.npc.dead
  (:require [moon.entity :as entity]))

(defmethods :npc-dead
  {:let {:keys [eid]}}
  (entity/->v [[_ eid]]
    {:eid eid})

  (entity/enter [_]
    [[:e/destroy eid]]))
