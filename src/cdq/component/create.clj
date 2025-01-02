(ns cdq.component.create
  (:require [anvil.entity :as entity]
            [anvil.entity.skills :as skills]
            [cdq.context :refer [timer finished-ratio]]
            [cdq.inventory :as inventory]
            [clojure.component :as component]
            [clojure.utils :refer [safe-merge]]
            [gdl.context :as c]
            [gdl.graphics.animation :as animation]
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

(defmethod component/create! :entity/animation
  [[_ animation] eid c]
  (swap! eid assoc :entity/image (animation/current-frame animation)))

(defmethod component/create! :entity/delete-after-animation-stopped?
  [_ eid c]
  (-> @eid :entity/animation :looping? not assert))

(defmethod component/create :entity/delete-after-duration
  [[_ duration] c]
  (timer c duration))

(defmethod component/create! :entity/fsm
  [[k {:keys [fsm initial-state]}] eid c]
  (swap! eid assoc
         k (->init-fsm (case fsm
                         :fsms/player player-fsm
                         :fsms/npc npc-fsm)
                       initial-state)
         initial-state (component/create [initial-state eid] c)))

(defmethod component/create :entity/hp
  [[_ v] c]
  [v v])

(defmethod component/create :entity/mana
  [[_ v] c]
  [v v])

(defmethod component/create :entity/projectile-collision
  [[_ v] c]
  (assoc v :already-hit-bodies #{}))

(defmethod component/create! :entity/skills
  [[k skills] eid c]
  (swap! eid assoc k nil)
  (doseq [skill skills]
    (skills/add c eid skill)))

(defmethod component/create! :entity/inventory
  [[k items] eid c]
  (swap! eid assoc k inventory/empty-inventory)
  (doseq [item items]
    (entity/pickup-item c eid item)))
