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

(defmethod component/create :stunned
  [[_ eid duration] c]
  {:eid eid
   :counter (timer c duration)})

(defmethod component/create :player-moving
  [[_ eid movement-vector] c]
  {:eid eid
   :movement-vector movement-vector})

(defmethod component/create :player-item-on-cursor
  [[_ eid item] c]
  (safe-merge (c/build c :player-item-on-cursor/component)
              {:eid eid
               :item item}))

(defmethod component/create :player-idle
  [[_ eid] c]
  (safe-merge (c/build c :player-idle/clicked-inventory-cell)
              {:eid eid}))

(defmethod component/create :player-dead
  [[k] c]
  (c/build c :player-dead/component.enter))

(defmethod component/create :npc-sleeping
  [[_ eid] c]
  {:eid eid})

(defmethod component/create :npc-moving
  [[_ eid movement-vector] c]
  {:eid eid
   :movement-vector movement-vector
   :counter (timer c (* (entity/stat @eid :entity/reaction-time) 0.016))})

(defmethod component/create :npc-idle
  [[_ eid] c]
  {:eid eid})

(defmethod component/create :npc-dead
  [[_ eid] c]
  {:eid eid})

(defn- apply-action-speed-modifier [entity skill action-time]
  (/ action-time
     (or (entity/stat entity (:skill/action-time-modifier-key skill))
         1)))

(defmethod component/create :active-skill
  [[_ eid [skill effect-ctx]] c]
  {:eid eid
   :skill skill
   :effect-ctx effect-ctx
   :counter (->> skill
                 :skill/action-time
                 (apply-action-speed-modifier @eid skill)
                 (timer c))})
