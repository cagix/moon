(ns cdq.install
  (:require [gdl.utils :refer [install]]
            [cdq.entity :as entity]
            [cdq.entity.state :as state]))

(def entity
  {:optional [; #'cdq.context.info/segment
              ; #'entity/create
              ; #'world/create!
              ; #'world/destroy!
              ; #'entity/tick
              ; cdq.entity.render/below/default/above/info
              ]})

(def entity-state
  (merge-with concat
              entity
              {:optional [#'state/enter
                          #'state/exit
                          #'state/cursor
                          #'state/pause-game?
                          #'state/manual-tick
                          #'state/clicked-inventory-cell
                          #'state/clicked-skillmenu-skill
                          ;#'state/draw-gui-view
                          ]}))

; TODO tests ! - all implemented/wired correctly/etc?
; TODO tests also interesting .... !
; keep running , show green/etc

(doseq [[ns-sym k] '{cdq.entity.state.active-skill :active-skill
                     cdq.entity.state.npc-dead :npc-dead
                     cdq.entity.state.npc-idle :npc-idle
                     cdq.entity.state.npc-moving :npc-moving
                     cdq.entity.state.npc-sleeping :npc-sleeping
                     cdq.entity.state.player-dead :player-dead
                     cdq.entity.state.player-idle :player-idle
                     cdq.entity.state.player-item-on-cursor :player-item-on-cursor
                     cdq.entity.state.player-moving :player-moving
                     cdq.entity.state.stunned :stunned}]
  (install entity-state ns-sym k))

(def effect {:required [#'cdq.effect/applicable?
                        #'cdq.effect/handle]
             :optional [;cdq.context.info/segment
                        #'cdq.effect/useful?
                        ;#'cdq.entity.render/render-effect
                        ]})

(doseq [[ns-sym k] '{cdq.effect.target-all :effects/target-all
                     cdq.effect.target-entity :effects/target-entity
                     cdq.effect.audiovisual :effects/audiovisual
                     cdq.effect.spawn :effects/spawn
                     cdq.effect.projectile :effects/projectile
                     cdq.effect.sound :effects/sound

                     cdq.effect.target.audiovisual :effects.target/audiovisual
                     cdq.effect.target.convert :effects.target/convert
                     cdq.effect.target.damage :effects.target/damage
                     cdq.effect.target.kill :effects.target/kill
                     cdq.effect.target.melee-damage :effects.target/melee-damage
                     cdq.effect.target.spiderweb :effects.target/spiderweb
                     cdq.effect.target.stun :effects.target/stun}]
  (install effect ns-sym k))
