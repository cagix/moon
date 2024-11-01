(ns moon.components
  (:require (moon.effect projectile
                         spawn
                         target-all
                         target-entity)
            (moon.effect.entity convert
                                damage
                                kill
                                melee-damage
                                spiderweb
                                stun)
            (moon.entity.npc dead
                             idle
                             moving
                             sleeping)
            (moon.entity.player dead
                                idle
                                item-on-cursor
                                moving)
            (moon.entity active
                         animation
                         clickable
                         delete-after-animation-stopped
                         delete-after-duration
                         destroy-audiovisual
                         faction
                         follow-ai
                         fsm
                         hitpoints
                         image
                         inventory
                         line-render
                         modifiers
                         mouseover
                         movement
                         player
                         projectile
                         skills
                         string-effect
                         stunned
                         temp-modifier)
            (moon.fsms player
                       npc)
            (moon.level generate
                        uf-caves
                        tiled-map)
            (moon.operation inc
                            mult
                            val-max)
            (moon.properties audiovisuals
                             creatures
                             items
                             projectiles
                             skills
                             worlds)
            (moon.schema animation
                         boolean
                         enum
                         image
                         map
                         number
                         one-to-many
                         one-to-one
                         sound
                         string
                         val-max)
            (moon.screens editor
                          main
                          map-editor
                          minimap
                          world)
            (moon.tx audiovisual
                     creature
                     cursor
                     effect
                     entity
                     item
                     line-render
                     projectile
                     sound
                     spawn-creatures)
            (moon.widgets action-bar
                          dev-menu
                          entity-info-window
                          hp-mana
                          inventory
                          player-message
                          player-modal
                          properties-overview
                          properties-tabs
                          property)
            moon.colors
            moon.world.widgets))
