(ns cdq.entity.state.stunned
  (:require [cdq.entity :as entity]
            [cdq.state :as state]
            [cdq.timer :as timer]
            [cdq.utils :refer [defmethods]]))

(defmethods :stunned
  (entity/create [[_ _eid duration] {:keys [ctx/elapsed-time]}]
    {:counter (timer/create elapsed-time duration)})

  (entity/tick! [[_ {:keys [counter]}] eid {:keys [ctx/elapsed-time]}]
    (when (timer/stopped? elapsed-time counter)
      [[:tx/event eid :effect-wears-off]]))

  (state/cursor [_ _eid _ctx] :cursors/denied))
