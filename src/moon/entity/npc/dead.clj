(ns moon.entity.npc.dead
  (:require [moon.component :refer [defc]]
            [moon.entity :as entity]))

(defc :npc-dead
  {:let {:keys [eid]}}
  (entity/->v [[_ eid]]
    {:eid eid})

  (entity/enter [_]
    [[:e/destroy eid]]))
