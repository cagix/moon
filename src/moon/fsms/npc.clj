(ns moon.fsms.npc
  (:require [moon.component :as component]
            [reduce-fsm :as fsm]))

(defc :fsms/npc
  (component/create [_]
    (fsm/defsm-inc npc
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
       [:npc-dead]])))
