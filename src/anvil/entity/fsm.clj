(ns ^:no-doc anvil.entity.fsm
  (:require [anvil.component :as component]
            [anvil.entity :as entity]
            [gdl.context :as c]
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

; fsm throws when initial-state is not part of states, so no need to assert initial-state
; initial state is nil, so associng it. make bug report at reduce-fsm?
(defn- ->init-fsm [fsm initial-state]
  (assoc (fsm initial-state nil) :state initial-state))

(defmethods :entity/fsm
  (component/info [[_ fsm]]
    (str "State: " (name (:state fsm))))


  (component/create [[k {:keys [fsm initial-state]}] eid]
    (swap! eid assoc
           k (->init-fsm (case fsm
                           :fsms/player player-fsm
                           :fsms/npc npc-fsm)
                         initial-state)
           initial-state (component/->v [initial-state eid]))))

(defmethod component/cursor :stunned               [_] :cursors/denied)
(defmethod component/cursor :player-moving         [_] :cursors/walking)
(defmethod component/cursor :player-item-on-cursor [_] :cursors/hand-grab)
(defmethod component/cursor :player-dead           [_] :cursors/black-x)
(defmethod component/cursor :active-skill          [_] :cursors/sandclock)

(defn-impl entity/state-k [entity]
  (-> entity :entity/fsm :state))

(defn-impl entity/state-obj [entity]
  (let [k (entity/state-k entity)]
    [k (k entity)]))

(defn- send-event! [c eid event params]
  (when-let [fsm (:entity/fsm @eid)]
    (let [old-state-k (:state fsm)
          new-fsm (fsm/fsm-event fsm event)
          new-state-k (:state new-fsm)]
      (when-not (= old-state-k new-state-k)
        (let [old-state-obj (entity/state-obj @eid)
              new-state-obj [new-state-k (component/->v (if params
                                                          [new-state-k eid params]
                                                          [new-state-k eid]))]]
          (when (:entity/player? @eid)
            (when-let [cursor (component/cursor new-state-obj)]
              (c/set-cursor c cursor)))
          (swap! eid #(-> %
                          (assoc :entity/fsm new-fsm
                                 new-state-k (new-state-obj 1))
                          (dissoc old-state-k)))
          (component/exit old-state-obj)
          (component/enter new-state-obj))))))

(defn-impl entity/event
  ([eid event]
   (send-event! (c/get-ctx) eid event nil))
  ([eid event params]
   (send-event! (c/get-ctx) eid event params)))
