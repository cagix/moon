(ns cdq.entity.state.stunned
  (:require [cdq.context :as world]
            [gdl.context.timer :as timer]
            [gdl.context :as c]))

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

(defn render-below [_ entity c]
  (c/circle c (:position entity) 0.5 [1 1 1 0.6]))
