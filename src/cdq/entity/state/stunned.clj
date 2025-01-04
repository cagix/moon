(ns cdq.entity.state.stunned
  (:require [cdq.context :as world]
            [gdl.context.timer :as timer]))

(defn cursor [_]
  :cursors/denied)

(defn pause-game? [_]
  false)

(defn create [[_ eid duration] c]
  {:eid eid
   :counter (timer/create c duration)})

(defn tick [[_ {:keys [counter]}] eid c]
  (when (timer/stopped? c counter)
    (world/send-event! c eid :effect-wears-off)))
