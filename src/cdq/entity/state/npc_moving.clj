(ns cdq.entity.state.npc-moving
  (:require [cdq.entity :as entity]
            [cdq.state :as state]
            [cdq.timer :as timer]
            [cdq.utils :refer [defcomponent]]))

(defcomponent :npc-moving
  (entity/create [[_ eid movement-vector] {:keys [ctx/elapsed-time]}]
    {:movement-vector movement-vector
     :counter (timer/create elapsed-time (* (entity/stat @eid :entity/reaction-time) 0.016))})

  (entity/tick! [[_ {:keys [counter]}]
                 eid
                 {:keys [ctx/elapsed-time]}]
    (when (timer/stopped? elapsed-time counter)
      [[:tx/event eid :timer-finished]]))

  (state/enter! [[_ {:keys [movement-vector]}] eid]
    [[:tx/set-movement eid movement-vector]])

  (state/exit! [_ eid _ctx]
    [[:tx/dissoc eid :entity/movement]]))
