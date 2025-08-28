(ns cdq.start
  (:require cdq.core
            cdq.utils.multifn))

(defn- install-effects! []
  (cdq.utils.multifn/add-methods! `[{:required [cdq.effect/applicable?
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
  (cdq.utils.multifn/add-methods! `[{:required [cdq.ctx.tx-handler/do!]}
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

(defn -main []
  (install-effects!)
  (install-txs!)
  (cdq.core/load! "cdq.app.edn"))
