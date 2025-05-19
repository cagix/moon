(ns cdq.entity.state.stunned
  (:require [cdq.ctx :as ctx]
            [cdq.draw :as draw]
            [cdq.entity :as entity]
            [cdq.state :as state]
            [cdq.timer :as timer]
            [cdq.utils :refer [defcomponent]]))

(defcomponent :stunned
  (entity/create [[_ _eid duration]]
    {:counter (timer/create ctx/elapsed-time duration)})

  (entity/tick! [[_ {:keys [counter]}] eid]
    (when (timer/stopped? ctx/elapsed-time counter)
      [[:tx/event eid :effect-wears-off]]))

  (state/cursor [_] :cursors/denied)

  (state/pause-game? [_] false)

  (entity/render-below! [_ entity]
    (draw/circle (:position entity) 0.5 [1 1 1 0.6])))



