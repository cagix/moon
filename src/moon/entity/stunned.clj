(ns moon.entity.stunned
  (:require [gdl.graphics.shape-drawer :as sd]
            [moon.entity :as entity]
            [moon.world.time :refer [timer stopped?]]))

(defmethods :stunned
  {:let {:keys [eid counter]}}
  (entity/->v [[_ eid duration]]
    {:eid eid
     :counter (timer duration)})

  (entity/player-enter [_]
    [[:tx/cursor :cursors/denied]])

  (entity/pause-game? [_]
    false)

  (entity/tick [_ eid]
    (when (stopped? counter)
      [[:entity/fsm eid :effect-wears-off]]))

  (entity/render-below [_ entity]
    (sd/circle (:position entity) 0.5 [1 1 1 0.6])))
