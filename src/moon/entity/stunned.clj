(ns moon.entity.stunned
  (:require [gdl.graphics.shape-drawer :as sd]
            [moon.entity.fsm :as fsm]
            [moon.world.time :refer [timer stopped?]]))

(defn ->v [eid duration]
  {:eid eid
   :counter (timer duration)})

(defn cursor [_]
  :cursors/denied)

(defn pause-game? [_]
  false)

(defn tick [{:keys [counter]} eid]
  (when (stopped? counter)
    (fsm/event eid :effect-wears-off)))

(defn render-below [_ entity]
  (sd/circle (:position entity) 0.5 [1 1 1 0.6]))
