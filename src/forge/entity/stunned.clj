(ns ^:no-doc forge.entity.stunned
  (:require [forge.entity.components :as entity]
            [forge.world :refer [timer stopped?]]))

(defn ->v [eid duration]
  {:eid eid
   :counter (timer duration)})

(defn cursor [_]
  :cursors/denied)

(defn pause-game? [_]
  false)

(defn tick [{:keys [counter]} eid]
  (when (stopped? counter)
    (entity/event eid :effect-wears-off)))
