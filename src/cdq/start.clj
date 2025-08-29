(ns cdq.start
  (:require cdq.application ; ok ... only used in UI ! or dev ! and cdq.start ! otherwise use post-runnable!
            ; == documentation !?

            cdq.create.extend-protocols ; start here ... at the top
            ; -> cdq.ctx -> should be in cdq.game ?!
            ; -> start with render which uses mostly ctx ...


            cdq.game
            cdq.utils.multifn
            clojure.gdx.backends.lwjgl

            cdq.entity.animation
            cdq.entity.body
            cdq.entity.delete-after-animation-stopped
            cdq.entity.delete-after-duration
            cdq.entity.destroy-audiovisual
            cdq.entity.fsm
            cdq.entity.inventory
            cdq.entity.projectile-collision
            cdq.entity.skills
            cdq.entity.stats
            )
  (:gen-class))

(defn- install-entity-components! []
  (.bindRoot #'cdq.world/entity-components
             {:entity/animation                       {:create   cdq.entity.animation/create}
              :entity/body                            {:create   cdq.entity.body/create}
              :entity/delete-after-animation-stopped? {:create!  cdq.entity.delete-after-animation-stopped/create!}
              :entity/delete-after-duration           {:create   cdq.entity.delete-after-duration/create}
              :entity/projectile-collision            {:create   cdq.entity.projectile-collision/create}
              :creature/stats                         {:create   cdq.entity.stats/create}
              :entity/fsm                             {:create!  cdq.entity.fsm/create!}
              :entity/inventory                       {:create!  cdq.entity.inventory/create!}
              :entity/skills                          {:create!  cdq.entity.skills/create!}
              :entity/destroy-audiovisual             {:destroy! cdq.entity.destroy-audiovisual/destroy!}}))

(defn- install-effects! []
  (cdq.utils.multifn/add-methods! '[{:required [cdq.effect/applicable?
                                                cdq.effect/handle]
                                     :optional [cdq.effect/useful?
                                                cdq.effect/render]}
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
  (cdq.utils.multifn/add-methods! '[{:required [cdq.game/do!]}
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
  (install-entity-components!)
  (install-effects!)
  (install-txs!)
  (install-editor-widgets!)
  (cdq.create.extend-protocols/do! cdq.game.Context 'cdq.ctx)
  (clojure.gdx.backends.lwjgl/start-application!
   {:title "Cyber Dungeon Quest"
    :windowed-mode {:width 1440 :height 900}
    :foreground-fps 60}
   {:create! (fn [context]
               (reset! cdq.application/state (cdq.game/create! context)))
    :dispose! (fn []
                (cdq.game/dispose! @cdq.application/state))
    :render! (fn []
               (swap! cdq.application/state cdq.game/render!))
    :resize! (fn [width height]
               (cdq.game/resize! @cdq.application/state width height))}))
