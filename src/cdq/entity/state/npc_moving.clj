(ns cdq.entity.state.npc-moving
  (:require [cdq.ctx :as ctx]
            [cdq.entity :as entity]
            [cdq.state :as state]
            [cdq.timer :as timer]
            [cdq.utils :refer [defcomponent]]))

(defcomponent :npc-moving
  (entity/create [[_ eid movement-vector]]
    {:eid eid
     :movement-vector movement-vector
     :counter (timer/create ctx/elapsed-time (* (entity/stat @eid :entity/reaction-time) 0.016))})

  (entity/tick! [[_ {:keys [counter]}] eid]
    (when (timer/stopped? ctx/elapsed-time counter)
      [[:tx/event eid :timer-finished]]))

  (state/enter! [[_ {:keys [eid movement-vector]}]]
    [[:tx/set-movement eid movement-vector]])

  (state/exit! [[_ {:keys [eid]}]]
    [[:tx/dissoc eid :entity/movement]]))
