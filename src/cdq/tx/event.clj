(ns cdq.tx.event
  (:require [cdq.entity.state :as state]
            [reduce-fsm :as fsm]))

(defn do!
  ([ctx eid event]
   (do! ctx eid event nil))
  ([ctx eid event params]
   (let [fsm (:entity/fsm @eid)
         _ (assert fsm)
         old-state-k (:state fsm)
         new-fsm (fsm/fsm-event fsm event)
         new-state-k (:state new-fsm)]
     (when-not (= old-state-k new-state-k)
       (let [old-state-obj (let [k (:state (:entity/fsm @eid))]
                             [k (k @eid)])
             new-state-obj [new-state-k (state/create ctx new-state-k eid params)]]
         [[:tx/assoc eid :entity/fsm new-fsm]
          [:tx/assoc eid new-state-k (new-state-obj 1)]
          [:tx/dissoc eid old-state-k]
          [:tx/state-exit eid old-state-obj]
          [:tx/state-enter eid new-state-obj]])))))
