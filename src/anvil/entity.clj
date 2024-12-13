(ns anvil.entity
  (:require [anvil.component :as component]
            [anvil.entity.inventory :as inventory]
            [anvil.entity.stat :as stat]
            [anvil.entity.skills :as skills]
            [anvil.world :refer [timer]]
            [gdl.graphics.animation :as animation]
            [reduce-fsm :as fsm]))

(defn- apply-action-speed-modifier [entity skill action-time]
  (/ action-time
     (or (stat/->value entity (:skill/action-time-modifier-key skill))
         1)))

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

(defmethod component/->v :entity/delete-after-duration [[_ duration]]
  (timer duration))

(defmethod component/->v :entity/hp [[_ v]]
  [v v])

(defmethod component/->v :entity/mana [[_ v]]
  [v v])

(defmethod component/->v :entity/projectile-collision [[_ v]]
  (assoc v :already-hit-bodies #{}))

(defmethod component/->v :stunned [[_ eid duration]]
  {:eid eid
   :counter (timer duration)})

(defmethod component/->v :player-moving [[_ eid movement-vector]]
  {:eid eid
   :movement-vector movement-vector})

(defmethod component/->v :player-item-on-cursor [[_ eid item]]
  {:eid eid
   :item item})

(defmethod component/->v :player-idle [[_ eid]]
  {:eid eid})

(defmethod component/->v :npc-sleeping [[_ eid]]
  {:eid eid})

(defmethod component/->v :npc-moving [[_ eid movement-vector]]
  {:eid eid
   :movement-vector movement-vector
   :counter (timer (* (stat/->value @eid :entity/reaction-time) 0.016))})

(defmethod component/->v :npc-idle [[_ eid]]
  {:eid eid})

(defmethod component/->v :npc-dead [[_ eid]]
  {:eid eid})

(defmethod component/->v :active-skill [[_ eid [skill effect-ctx]]]
  {:eid eid
   :skill skill
   :effect-ctx effect-ctx
   :counter (->> skill
                 :skill/action-time
                 (apply-action-speed-modifier @eid skill)
                 timer)})

(defmethod component/create :entity/skills [[k skills] eid]
  (swap! eid assoc k nil)
  (doseq [skill skills]
    (swap! eid skills/add skill)))

(defmethod component/create :entity/inventory [[k items] eid]
  (swap! eid assoc k inventory/empty-inventory)
  (doseq [item items]
    (inventory/pickup-item eid item)))

(defmethod component/create :entity/delete-after-animation-stopped? [_ eid]
  (-> @eid :entity/animation :looping? not assert))

(defmethod component/create :entity/animation [[_ animation] eid]
  (swap! eid assoc :entity/image (animation/current-frame animation)))

; fsm throws when initial-state is not part of states, so no need to assert initial-state
; initial state is nil, so associng it. make bug report at reduce-fsm?
(defn- ->init-fsm [fsm initial-state]
  (assoc (fsm initial-state nil) :state initial-state))

(defmethod component/create :entity/fsm [[k {:keys [fsm initial-state]}] eid]
  (swap! eid assoc
         k (->init-fsm (case fsm
                         :fsms/player player-fsm
                         :fsms/npc npc-fsm)
                       initial-state)
         initial-state (component/->v [initial-state eid])))
