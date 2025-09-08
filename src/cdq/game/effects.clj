(ns cdq.game.effects
  (:require [cdq.multifn]))

(defn init! []
  (cdq.multifn/add-methods! '[{:required [cdq.effect/applicable?
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
