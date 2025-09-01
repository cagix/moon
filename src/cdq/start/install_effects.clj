(ns cdq.start.install-effects
  (:require cdq.multifn))

(defn do! []
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
