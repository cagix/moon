([draw-gui-view (:default :player-item-on-cursor)]
 [clicked-inventory-cell
  (:default :player-idle :player-item-on-cursor)]
 [enter!
  (:active-skill
   :default
   :npc-dead
   :npc-moving
   :player-dead
   :player-item-on-cursor
   :player-moving)]
 [pause-game?
  (:active-skill
   :player-dead
   :player-idle
   :player-item-on-cursor
   :player-moving
   :stunned)]
 [exit!
  (:default
   :npc-moving
   :npc-sleeping
   :player-item-on-cursor
   :player-moving)]
 [manual-tick (:default :player-idle :player-item-on-cursor)]
 [cursor
  (:active-skill
   :default
   :player-dead
   :player-item-on-cursor
   :player-moving
   :stunned)])
