(ns forge.entity.fsm
  (:require [forge.app.cursors :refer [set-cursor]]
            [forge.entity :refer [->v e-create]]
            [forge.entity.state :as state]
            [reduce-fsm :as fsm]))

(defn e-state-k [entity]
  (-> entity :entity/fsm :state))

(defn e-state-obj [entity]
  (let [k (e-state-k entity)]
    [k (k entity)]))

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
            (when-let [cursor (state/cursor new-state-obj)]
              (set-cursor cursor)))
          (swap! eid #(-> %
                          (assoc :entity/fsm new-fsm
                                 new-state-k (new-state-obj 1))
                          (dissoc old-state-k)))
          (state/exit old-state-obj)
          (state/enter new-state-obj))))))

(defn send-event
  ([eid event]
   (send-event! eid event nil))
  ([eid event params]
   (send-event! eid event params)))

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

; fsm throws when initial-state is not part of states, so no need to assert initial-state
; initial state is nil, so associng it. make bug report at reduce-fsm?
(defn- ->init-fsm [fsm initial-state]
  (assoc (fsm initial-state nil) :state initial-state))

(defmethod e-create :entity/fsm [[k {:keys [fsm initial-state]}] eid]
  (swap! eid assoc
         k (->init-fsm (case fsm
                         :fsms/player player-fsm
                         :fsms/npc npc-fsm)
                       initial-state)
         initial-state (->v [initial-state eid])))
