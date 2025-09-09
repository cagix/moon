(ns cdq.entity.tick)

(require 'cdq.effects)

(def function-map
  (cdq.effects/walk-method-map
   '{:entity/alert-friendlies-after-duration cdq.entity.alert-friendlies-after-duration/tick!
     :entity/animation cdq.entity.animation/tick!
     :entity/delete-after-animation-stopped? cdq.entity.delete-after-animation-stopped/tick!
     :entity/delete-after-duration cdq.entity.delete-after-duration/tick!
     :entity/movement cdq.entity.movement/tick!
     :entity/projectile-collision cdq.entity.projectile-collision/tick!
     :entity/skills cdq.entity.skills/tick!
     :active-skill cdq.entity.state.active-skill/tick!
     :npc-idle cdq.entity.state.npc-idle/tick!
     :npc-moving cdq.entity.state.npc-moving/tick!
     :npc-sleeping cdq.entity.state.npc-sleeping/tick!
     :stunned cdq.entity.state.stunned/tick!
     :entity/string-effect cdq.entity.string-effect/tick!
     :entity/temp-modifier cdq.entity.temp-modifier/tick!}))
