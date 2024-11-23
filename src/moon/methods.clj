(ns moon.methods
  (:require [moon.system :as system]
            [moon.systems.effect :as effect]
            [moon.systems.entity :as entity-sys]
            [moon.systems.entity-state :as state]
            moon.methods.info
            (moon.schema animation
                         boolean
                         enum
                         image
                         map
                         number
                         one-to-many
                         one-to-one
                         sound
                         string)
            (moon.level generate
                        uf-caves
                        tiled-map)))

(def ^:private effect
  {:required [#'effect/applicable?
              #'effect/handle]
   :optional [#'effect/useful?
              #'effect/render]})

(system/install-all effect
                    '[moon.effects.projectile
                      moon.effects.spawn
                      moon.effects.target-all
                      moon.effects.target-entity

                      moon.effects.target.audiovisual
                      moon.effects.target.convert
                      moon.effects.target.damage
                      moon.effects.target.kill
                      moon.effects.target.melee-damage
                      moon.effects.target.spiderweb
                      moon.effects.target.stun])

(def ^:private entity
  {:optional [#'entity-sys/->v
              #'entity-sys/create
              #'entity-sys/destroy
              #'entity-sys/tick
              #'entity-sys/render-below
              #'entity-sys/render
              #'entity-sys/render-above
              #'entity-sys/render-info]})

(system/install-all entity
                    '[moon.entity.alert-friendlies-after-duration
                      moon.entity.animation
                      moon.entity.clickable
                      moon.entity.delete-after-animation-stopped
                      moon.entity.delete-after-duration
                      moon.entity.destroy-audiovisual
                      moon.entity.fsm
                      moon.entity.image
                      moon.entity.inventory
                      moon.entity.line-render
                      moon.entity.mouseover?
                      moon.entity.projectile-collision
                      moon.entity.skills
                      moon.entity.string-effect
                      moon.entity.movement
                      moon.entity.temp-modifier
                      moon.entity.hp
                      moon.entity.mana])

(def ^:private entity-state
  (merge-with concat
              entity
              {:optional [#'state/enter
                          #'state/exit
                          #'state/cursor
                          #'state/pause-game?
                          #'state/manual-tick
                          #'state/clicked-inventory-cell
                          #'state/clicked-skillmenu-skill
                          #'state/draw-gui-view]}))

(system/install entity-state 'moon.entity.npc.dead              :npc-dead)
(system/install entity-state 'moon.entity.npc.idle              :npc-idle)
(system/install entity-state 'moon.entity.npc.moving            :npc-moving)
(system/install entity-state 'moon.entity.npc.sleeping          :npc-sleeping)
(system/install entity-state 'moon.entity.player.dead           :player-dead)
(system/install entity-state 'moon.entity.player.idle           :player-idle)
(system/install entity-state 'moon.entity.player.item-on-cursor :player-item-on-cursor)
(system/install entity-state 'moon.entity.player.moving         :player-moving)
(system/install entity-state 'moon.entity.active                :active-skill)
(system/install entity-state 'moon.entity.stunned               :stunned)
