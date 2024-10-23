(ns world.creature.npc.dead
  (:require [moon.component :refer [defc]]
            [world.entity :as entity]
            [world.entity.state :as state]))

(defc :npc-dead
  {:let {:keys [eid]}}
  (entity/->v [[_ eid]]
    {:eid eid})

  (state/enter [_]
    [[:e/destroy eid]]))
