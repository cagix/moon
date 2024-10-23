(ns ^:no-doc moon.creature.stunned
  (:require [moon.component :refer [defc]]
            [moon.graphics :as g]
            [moon.world :refer [timer stopped?]]
            [moon.entity :as entity]
            [moon.entity.state :as state]))

(defc :stunned
  {:let {:keys [eid counter]}}
  (entity/->v [[_ eid duration]]
    {:eid eid
     :counter (timer duration)})

  (state/player-enter [_]
    [[:tx/cursor :cursors/denied]])

  (state/pause-game? [_]
    false)

  (entity/tick [_ eid]
    (when (stopped? counter)
      [[:tx/event eid :effect-wears-off]]))

  (entity/render-below [_ entity]
    (g/draw-circle (:position entity) 0.5 [1 1 1 0.6])))
