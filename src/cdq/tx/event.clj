(ns cdq.tx.event
  (:require [cdq.ctx :as ctx]
            [cdq.entity :as entity]
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
             (ctx/handle-txs! ((:state-changed! (:entity/player? @eid)) new-state-obj)))
           (swap! eid #(-> %
                           (assoc :entity/fsm new-fsm
                                  new-state-k (new-state-obj 1))
                           (dissoc old-state-k)))
           (ctx/handle-txs! (state/exit!  old-state-obj eid ctx))
           (ctx/handle-txs! (state/enter! new-state-obj eid))))))))
