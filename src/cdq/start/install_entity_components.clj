(ns cdq.start.install-entity-components
  (:require cdq.entity-api
            cdq.render-layers
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

(defn do! [_]
  (.bindRoot #'cdq.entity-api/render-layers
             [{:entity/mouseover? cdq.render-layers/draw-mouseover-highlighting
               :stunned cdq.render-layers/draw-stunned-state
               :player-item-on-cursor cdq.render-layers/draw-item-on-cursor-state}
              {:entity/clickable cdq.render-layers/draw-clickable-mouseover-text
               :entity/animation cdq.render-layers/call-render-image
               :entity/image cdq.render-layers/draw-centered-rotated-image
               :entity/line-render cdq.render-layers/draw-line-entity}
              {:npc-sleeping cdq.render-layers/draw-sleeping-state
               :entity/temp-modifier cdq.render-layers/draw-temp-modifiers
               :entity/string-effect cdq.render-layers/draw-text-over-entity}
              {:creature/stats cdq.render-layers/draw-stats
               :active-skill cdq.render-layers/draw-active-skill}])
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

  (.bindRoot #'cdq.entity-api/entity->tick
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
