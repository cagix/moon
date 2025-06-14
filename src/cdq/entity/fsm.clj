(ns cdq.entity.fsm
  (:require [cdq.entity :as entity]
            [cdq.utils :refer [defmethods]]
            [reduce-fsm :as fsm]))

(def ^:private npc-fsm
  (fsm/fsm-inc
   [[:npc-sleeping
     :kill -> :npc-dead
     :stun -> :stunned
     :alert -> :npc-idle]
    [:npc-idle
     :kill -> :npc-dead
     :stun -> :stunned
     :start-action -> :active-skill
     :movement-direction -> :npc-moving]
    [:npc-moving
     :kill -> :npc-dead
     :stun -> :stunned
     :timer-finished -> :npc-idle]
    [:active-skill
     :kill -> :npc-dead
     :stun -> :stunned
     :action-done -> :npc-idle]
    [:stunned
     :kill -> :npc-dead
     :effect-wears-off -> :npc-idle]
    [:npc-dead]]))

(def ^:private player-fsm
  (fsm/fsm-inc
   [[:player-idle
     :kill -> :player-dead
     :stun -> :stunned
     :start-action -> :active-skill
     :pickup-item -> :player-item-on-cursor
     :movement-input -> :player-moving]
    [:player-moving
     :kill -> :player-dead
     :stun -> :stunned
     :no-movement-input -> :player-idle]
    [:active-skill
     :kill -> :player-dead
     :stun -> :stunned
     :action-done -> :player-idle]
    [:stunned
     :kill -> :player-dead
     :effect-wears-off -> :player-idle]
    [:player-item-on-cursor
     :kill -> :player-dead
     :stun -> :stunned
     :drop-item -> :player-idle
     :dropped-item -> :player-idle]
    [:player-dead]]))

(defmethods :entity/fsm
  (entity/create! [[k {:keys [fsm initial-state]}] eid ctx]
    ; fsm throws when initial-state is not part of states, so no need to assert initial-state
    ; initial state is nil, so associng it. make bug report at reduce-fsm?
    [[:tx/assoc eid k (assoc ((case fsm
                                :fsms/player player-fsm
                                :fsms/npc npc-fsm) initial-state nil) :state initial-state)]
     [:tx/assoc eid initial-state (entity/create [initial-state eid]
                                                 ctx)]]))

(defn event->txs [ctx eid event params]
  (let [fsm (:entity/fsm @eid)
        _ (assert fsm)
        old-state-k (:state fsm)
        new-fsm (fsm/fsm-event fsm event)
        new-state-k (:state new-fsm)]
    (when-not (= old-state-k new-state-k)
      (let [old-state-obj (let [k (:state (:entity/fsm @eid))]
                            [k (k @eid)])
            new-state-obj [new-state-k (entity/create (if params
                                                        [new-state-k eid params]
                                                        [new-state-k eid])
                                                      ctx)]]
        [[:tx/assoc eid :entity/fsm new-fsm]
         [:tx/assoc eid new-state-k (new-state-obj 1)]
         [:tx/dissoc eid old-state-k]
         [:tx/state-exit eid old-state-obj]
         [:tx/state-enter eid new-state-obj]]))))
