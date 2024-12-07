(ns forge.entity.state.stunned
  (:require [clojure.utils :refer [defmethods]]
            [forge.app.shape-drawer :as sd]
            [forge.entity :refer [->v render-below tick]]
            [forge.entity.fsm :refer [send-event]]
            [forge.entity.state :refer [cursor pause-game?]]
            [forge.world.time :refer [timer stopped?]]))

(defmethods :stunned
  (->v [[_ eid duration]]
    {:eid eid
     :counter (timer duration)})

  (cursor [_]
    :cursors/denied)

  (pause-game? [_]
    false)

  (tick [[_ {:keys [counter]}] eid]
    (when (stopped? counter)
      (send-event eid :effect-wears-off)))

  (render-below [_ entity]
    (sd/circle (:position entity) 0.5 [1 1 1 0.6])))
