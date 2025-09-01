(ns cdq.start
  (:require cdq.start.gdx-app
            cdq.start.set-icon
            cdq.multifn
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
            cdq.entity.temp-modifier)
  (:gen-class))

(defn- install-entity-components! []
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

(defn- install-effects! []
  (cdq.multifn/add-methods! '[{:required [cdq.world.effect/applicable?
                                          cdq.world.effect/handle]
                               :optional [cdq.world.effect/useful?
                                          cdq.world.effect/render]}
                              [[cdq.effects.audiovisual
                                :effects/audiovisual]
                               [cdq.effects.projectile
                                :effects/projectile]
                               [cdq.effects.sound
                                :effects/sound]
                               [cdq.effects.spawn
                                :effects/spawn]
                               [cdq.effects.target-all
                                :effects/target-all]
                               [cdq.effects.target-entity
                                :effects/target-entity]
                               [cdq.effects.target.audiovisual
                                :effects.target/audiovisual]
                               [cdq.effects.target.convert
                                :effects.target/convert]
                               [cdq.effects.target.damage
                                :effects.target/damage]
                               [cdq.effects.target.kill
                                :effects.target/kill]
                               [cdq.effects.target.melee-damage
                                :effects.target/melee-damage]
                               [cdq.effects.target.spiderweb
                                :effects.target/spiderweb]
                               [cdq.effects.target.stun
                                :effects.target/stun]]]))

(defn- install-txs! []
  (cdq.multifn/add-methods! '[{:required [cdq.game/do!]}
                              [[cdq.tx.toggle-inventory-visible
                                :tx/toggle-inventory-visible]
                               [cdq.tx.show-message
                                :tx/show-message]
                               [cdq.tx.show-modal
                                :tx/show-modal]
                               [cdq.tx.sound
                                :tx/sound]
                               [cdq.tx.state-exit
                                :tx/state-exit]
                               [cdq.tx.state-enter
                                :tx/state-enter]
                               [cdq.tx.audiovisual
                                :tx/audiovisual]
                               [cdq.tx.spawn-alert
                                :tx/spawn-alert]
                               [cdq.tx.spawn-line
                                :tx/spawn-line]
                               [cdq.tx.deal-damage
                                :tx/deal-damage]
                               [cdq.tx.set-movement
                                :tx/set-movement]
                               [cdq.tx.move-entity
                                :tx/move-entity]
                               [cdq.tx.spawn-projectile
                                :tx/spawn-projectile]
                               [cdq.tx.spawn-effect
                                :tx/spawn-effect]
                               [cdq.tx.spawn-item
                                :tx/spawn-item]
                               [cdq.tx.spawn-creature
                                :tx/spawn-creature]]]))

(defn- install-editor-widgets! []
  (run! require '[cdq.ui.editor.widget.default
                  cdq.ui.editor.widget.edn
                  cdq.ui.editor.widget.string
                  cdq.ui.editor.widget.boolean
                  cdq.ui.editor.widget.enum
                  cdq.ui.editor.widget.sound
                  cdq.ui.editor.widget.one-to-one
                  cdq.ui.editor.widget.one-to-many
                  cdq.ui.editor.widget.image
                  cdq.ui.editor.widget.animation
                  cdq.ui.editor.widget.map]))

(defn -main []
  (doseq [f [install-entity-components!
             install-effects!
             install-txs!
             install-editor-widgets!
             cdq.start.set-icon/do!
             cdq.start.gdx-app/do!]]
    (f)))
