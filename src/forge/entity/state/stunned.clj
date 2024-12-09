(ns forge.entity.state.stunned
  (:require [anvil.graphics :as g]
            [forge.entity.fsm :refer [send-event]]
            [forge.world.time :refer [timer stopped?]]))

(defn ->v [[_ eid duration]]
  {:eid eid
   :counter (timer duration)})

(defn cursor [_]
  :cursors/denied)

(defn pause-game? [_]
  false)

(defn tick [[_ {:keys [counter]}] eid]
  (when (stopped? counter)
    (send-event eid :effect-wears-off)))

(defn render-below [_ entity]
  (g/circle (:position entity) 0.5 [1 1 1 0.6]))
