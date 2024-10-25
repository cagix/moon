(ns ^:no-doc moon.entity.npc.dead
  (:require [moon.component :refer [defc]]
            [moon.entity :as entity]
            [moon.entity.state :as state]))

(defc :npc-dead
  {:let {:keys [eid]}}
  (entity/->v [[_ eid]]
    {:eid eid})

  (state/enter [_]
    [[:e/destroy eid]]))
