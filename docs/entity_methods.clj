([render-info! (:active-skill :default :entity/hp)]
 [render-below!
  (:default :player-item-on-cursor :stunned :entity/mouseover?)]
 [destroy! (:default :entity/destroy-audiovisual)]
 [create!
  (:default
   :entity/animation
   :entity/delete-after-animation-stopped?
   :entity/fsm
   :entity/inventory
   :entity/player?
   :entity/skills)]
 [create
  (:active-skill
   :default
   :npc-dead
   :npc-idle
   :npc-moving
   :npc-sleeping
   :player-idle
   :player-item-on-cursor
   :player-moving
   :stunned
   :entity/delete-after-duration
   :entity/hp
   :entity/mana
   :entity/projectile-collision)]
 [tick!
  (:active-skill
   :default
   :npc-idle
   :npc-moving
   :npc-sleeping
   :player-moving
   :stunned
   :entity/alert-friendlies-after-duration
   :entity/animation
   :entity/delete-after-animation-stopped?
   :entity/delete-after-duration
   :entity/movement
   :entity/projectile-collision
   :entity/skills
   :entity/string-effect
   :entity/temp-modifier)]
 [render-default!
  (:default :entity/clickable :entity/image :entity/line-render)]
 [render-above!
  (:default :npc-sleeping :entity/string-effect :entity/temp-modifier)])
