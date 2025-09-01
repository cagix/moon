(ns cdq.start.install-entity-components
  (:require cdq.game
            cdq.ctx.world
            cdq.entity.alert-friendlies-after-duration
            cdq.entity.animation
            cdq.entity.body
            cdq.entity.delete-after-animation-stopped
            cdq.entity.delete-after-duration
            cdq.entity.destroy-audiovisual
            cdq.entity.fsm
            cdq.entity.inventory
            cdq.entity.projectile-collision
            cdq.entity.movement
            cdq.entity.skills
            cdq.entity.stats
            cdq.entity.state.active-skill
            cdq.entity.state.npc-idle
            cdq.entity.state.npc-moving
            cdq.entity.state.npc-sleeping
            cdq.entity.state.stunned
            cdq.entity.string-effect
            cdq.entity.temp-modifier))

(defn do! []
  (.bindRoot #'cdq.ctx.world/entity-components
             {:entity/animation                       {:create   cdq.entity.animation/create}
              :entity/body                            {:create   cdq.entity.body/create}
              :entity/delete-after-animation-stopped? {:create!  cdq.entity.delete-after-animation-stopped/create!}
              :entity/delete-after-duration           {:create   cdq.entity.delete-after-duration/create}
              :entity/projectile-collision            {:create   cdq.entity.projectile-collision/create}
              :creature/stats                         {:create   cdq.entity.stats/create}
              :entity/fsm                             {:create!  cdq.entity.fsm/create!}
              :entity/inventory                       {:create!  cdq.entity.inventory/create!}
              :entity/skills                          {:create!  cdq.entity.skills/create!}
              :entity/destroy-audiovisual             {:destroy! cdq.entity.destroy-audiovisual/destroy!}})

  (.bindRoot #'cdq.game/entity->tick
             {:entity/alert-friendlies-after-duration cdq.entity.alert-friendlies-after-duration/tick!
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
