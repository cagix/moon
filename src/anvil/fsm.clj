(ns anvil.fsm
  (:require [anvil.entity :as entity]
            [anvil.graphics :as g]
            [clojure.component :refer [defsystem]]
            [reduce-fsm :as fsm]))

(defn state-k [entity]
  (-> entity :entity/fsm :state))

(defn state-obj [entity]
  (let [k (state-k entity)]
    [k (k entity)]))

(defsystem enter)
(defmethod enter :default [_])

(defsystem exit)
(defmethod exit :default [_])

(defsystem cursor)
(defmethod cursor :default [_])

(defmethod cursor :stunned [_]
  :cursors/denied)

(defn- send-event! [eid event params]
  (when-let [fsm (:entity/fsm @eid)]
    (let [old-state-k (:state fsm)
          new-fsm (fsm/fsm-event fsm event)
          new-state-k (:state new-fsm)]
      (when-not (= old-state-k new-state-k)
        (let [old-state-obj (state-obj @eid)
              new-state-obj [new-state-k (entity/->v (if params
                                                       [new-state-k eid params]
                                                       [new-state-k eid]))]]
          (when (:entity/player? @eid)
            (when-let [cursor-k (cursor new-state-obj)]
              (g/set-cursor cursor-k)))
          (swap! eid #(-> %
                          (assoc :entity/fsm new-fsm
                                 new-state-k (new-state-obj 1))
                          (dissoc old-state-k)))
          (exit old-state-obj)
          (enter new-state-obj))))))

(defn event
  ([eid event]
   (send-event! eid event nil))
  ([eid event params]
   (send-event! eid event params)))
