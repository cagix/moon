(ns moon.entity.stunned
  (:require [moon.app :refer [draw-circle]]
            [moon.entity.fsm :as fsm]
            [moon.world :refer [timer stopped?]]))

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
  (draw-circle (:position entity) 0.5 [1 1 1 0.6]))
