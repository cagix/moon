(ns forge.entity.fsm
  (:require [clojure.utils :refer [defsystem]]
            [forge.app.cursors :refer [set-cursor]]
            [forge.world :refer [->v]]
            [reduce-fsm :as fsm]))

(defn e-state-k [entity]
  (-> entity :entity/fsm :state))

(defn e-state-obj [entity]
  (let [k (e-state-k entity)]
    [k (k entity)]))

(defsystem state-enter)
(defmethod state-enter :default [_])

(defsystem state-exit)
(defmethod state-exit :default [_])

(defsystem state-cursor)
(defmethod state-cursor :default [_])

(defn- send-event! [eid event params]
  (when-let [fsm (:entity/fsm @eid)]
    (let [old-state-k (:state fsm)
          new-fsm (fsm/fsm-event fsm event)
          new-state-k (:state new-fsm)]
      (when-not (= old-state-k new-state-k)
        (let [old-state-obj (e-state-obj @eid)
              new-state-obj [new-state-k (->v (if params
                                                [new-state-k eid params]
                                                [new-state-k eid]))]]
          (when (:entity/player? @eid)
            (when-let [cursor (state-cursor new-state-obj)]
              (set-cursor cursor)))
          (swap! eid #(-> %
                          (assoc :entity/fsm new-fsm
                                 new-state-k (new-state-obj 1))
                          (dissoc old-state-k)))
          (state-exit old-state-obj)
          (state-enter new-state-obj))))))

(defn send-event
  ([eid event]
   (send-event! eid event nil))
  ([eid event params]
   (send-event! eid event params)))
