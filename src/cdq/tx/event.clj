(ns cdq.tx.event
  (:require [cdq.entity :as entity]
            [cdq.g :as g]
            [cdq.state :as state]
            [cdq.utils :as utils]
            [reduce-fsm :as fsm]))

(defn do!
  ([ctx eid event]
   (do! ctx eid event nil))
  ([ctx eid event params]
   (when-let [fsm (:entity/fsm @eid)]
     (let [old-state-k (:state fsm)
           new-fsm (fsm/fsm-event fsm event)
           new-state-k (:state new-fsm)]
       (when-not (= old-state-k new-state-k)
         (let [old-state-obj (entity/state-obj @eid)
               new-state-obj [new-state-k (entity/create (if params
                                                           [new-state-k eid params]
                                                           [new-state-k eid])
                                                         ctx)]]
           (when (:entity/player? @eid)
             (g/handle-txs! ctx ((:state-changed! (:entity/player? @eid)) new-state-obj)))
           (swap! eid #(-> %
                           (assoc :entity/fsm new-fsm
                                  new-state-k (new-state-obj 1))
                           (dissoc old-state-k)))
           (g/handle-txs! ctx (state/exit!  old-state-obj eid ctx))
           (g/handle-txs! ctx (state/enter! new-state-obj eid))))))))
