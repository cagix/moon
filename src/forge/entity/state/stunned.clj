(ns forge.entity.state.stunned
  (:require [anvil.fsm :as fsm]
            [anvil.graphics :as g]
            [anvil.world :refer [timer stopped?]]))

(defn ->v [[_ eid duration]]
  {:eid eid
   :counter (timer duration)})

(defn cursor [_]
  :cursors/denied)

(defn pause-game? [_]
  false)

(defn tick [[_ {:keys [counter]}] eid]
  (when (stopped? counter)
    (fsm/event eid :effect-wears-off)))

(defn render-below [_ entity]
  (g/circle (:position entity) 0.5 [1 1 1 0.6]))
