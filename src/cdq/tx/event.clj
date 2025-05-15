(ns cdq.tx.event
  (:require [cdq.entity :as entity]
            [cdq.entity.state :as state]
            [cdq.utils :as utils]
            [reduce-fsm :as fsm]))

(defn do!
  ([eid event]
   (do! eid event nil))
  ([eid event params]
   (when-let [fsm (:entity/fsm @eid)]
     (let [old-state-k (:state fsm)
           new-fsm (fsm/fsm-event fsm event)
           new-state-k (:state new-fsm)]
       (when-not (= old-state-k new-state-k)
         (let [old-state-obj (entity/state-obj @eid)
               new-state-obj [new-state-k (entity/create (if params
                                                           [new-state-k eid params]
                                                           [new-state-k eid]))]]
           (when (:entity/player? @eid)
             ((:state-changed! (:entity/player? @eid)) new-state-obj))
           (swap! eid #(-> %
                           (assoc :entity/fsm new-fsm
                                  new-state-k (new-state-obj 1))
                           (dissoc old-state-k)))
           (utils/handle-txs! (state/exit!  old-state-obj))
           (utils/handle-txs! (state/enter! new-state-obj))))))))
