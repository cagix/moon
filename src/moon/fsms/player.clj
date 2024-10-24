(ns moon.fsms.player
  (:require [moon.component :refer [defc] :as component]
            [reduce-fsm :as fsm]))

(defc :fsms/player
  (component/create [_]
    (fsm/defsm-inc player
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
       [:player-dead]])))
