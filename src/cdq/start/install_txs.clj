(ns cdq.start.install-txs
  (:require cdq.multifn))

(defn do! [_]
  (cdq.multifn/add-methods! '[{:required [cdq.ctx/do!]}
                              [[cdq.tx.add-skill
                                :tx/add-skill]
                               [cdq.tx.set-item
                                :tx/set-item]
                               [cdq.tx.remove-item
                                :tx/remove-item]
                               [cdq.tx.toggle-inventory-visible
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
