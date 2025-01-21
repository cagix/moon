(ns cdq.entity.fsm
  (:require [cdq.entity :as entity]
            [cdq.fsm :as fsm]
            cdq.graphics
            [cdq.utils :refer [defsystem]]))

(defsystem enter)
(defmethod enter :default [_ c])

(defsystem exit)
(defmethod exit :default [_ c])

(defn event
  ([c eid event*]
   (event c eid event* nil))
  ([c eid event params]
   (when-let [fsm (:entity/fsm @eid)]
     (let [old-state-k (:state fsm)
           new-fsm (fsm/event fsm event)
           new-state-k (:state new-fsm)]
       (when-not (= old-state-k new-state-k)
         (let [old-state-obj (entity/state-obj @eid)
               new-state-obj [new-state-k (entity/create (if params
                                                           [new-state-k eid params]
                                                           [new-state-k eid])
                                                         c)]]
           (when (:entity/player? @eid)
             (when-let [cursor (get-in c [:context/entity-states new-state-k :cursor])]
               (cdq.graphics/set-cursor c cursor)))
           (swap! eid #(-> %
                           (assoc :entity/fsm new-fsm
                                  new-state-k (new-state-obj 1))
                           (dissoc old-state-k)))
           (exit  old-state-obj c)
           (enter new-state-obj c)))))))
