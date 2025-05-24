(ns cdq.entity.state.stunned
  (:require [cdq.entity :as entity]
            [cdq.g :as g]
            [cdq.state :as state]
            [cdq.utils :refer [defcomponent]]))

(defcomponent :stunned
  (entity/create [[_ _eid duration] ctx]
    {:counter (g/create-timer ctx duration)})

  (entity/tick! [[_ {:keys [counter]}] eid ctx]
    (when (g/timer-stopped? ctx counter)
      [[:tx/event eid :effect-wears-off]]))

  (state/cursor [_] :cursors/denied)

  (state/pause-game? [_] false)

  (entity/render-below! [_ entity _ctx]
    [[:draw/circle (entity/position entity) 0.5 [1 1 1 0.6]]]))
