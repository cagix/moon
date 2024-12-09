(ns forge.install
  (:require [clojure.utils :refer [install install-component]]
            [anvil.app :as app]
            [anvil.system :as system]

            forge.schemas
            forge.info
            forge.mapgen.generate
            forge.mapgen.uf-caves))

(def screen {:optional [#'system/actors
                        #'app/enter
                        #'app/exit
                        #'system/render
                        #'system/dispose]})

(install "forge"
         screen
         (map vector [:screens/stage
                      :screens/editor
                      :screens/main-menu
                      :screens/map-editor
                      :screens/minimap
                      :screens/world]))

(install "forge"
         {:optional [#'app/create
                     #'app/dispose
                     #'app/render
                     #'app/resize]}
         (map vector [:app/db
                      :app/asset-manager
                      :app/sprite-batch
                      :app/shape-drawer
                      :app/default-font
                      :app/cached-map-renderer
                      :app/cursors
                      :app/vis-ui
                      :app/gui-viewport
                      :app/world-viewport
                      :app/screens]))

(install "forge"
         {:required [#'system/applicable?
                     #'system/handle]
          :optional [#'system/useful?
                     #'system/render-effect]}
         (map vector [:effects/projectile
                      :effects/spawn
                      :effects/target-all
                      :effects/target-entity

                      :effects.target/audiovisual
                      :effects.target/convert
                      :effects.target/damage
                      ;:effects.target/hp
                      :effects.target/kill
                      :effects.target/melee-damage
                      :effects.target/spiderweb
                      :effects.target/stun]))

(def entity
  {:optional [#'system/->v
              #'system/create
              #'system/destroy
              #'system/tick
              #'system/render-below
              #'system/render-default
              #'system/render-above
              #'system/render-info]})

(install "forge"
         entity
         (map vector [:entity/alert-friendlies-after-duration
                      :entity/animation
                      :entity/clickable
                      :entity/delete-after-animation-stopped?
                      :entity/delete-after-duration
                      :entity/destroy-audiovisual
                      :entity/fsm
                      :entity/hp
                      :entity/image
                      :entity/inventory
                      :entity/line-render
                      :entity/mana
                      :entity/mouseover?
                      :entity/movement
                      :entity/string-effect
                      :entity/skills
                      :entity/projectile-collision
                      :entity/temp-modifier]))

(def entity-state
  (merge-with concat
              entity
              {:optional [#'system/enter
                          #'system/exit
                          #'system/cursor
                          #'system/pause-game?
                          #'system/manual-tick
                          #'system/clicked-inventory-cell
                          #'system/clicked-skillmenu-skill
                          #'system/draw-gui-view]}))

(doseq [[ns-sym k] '[[forge.entity.state.active-skill :active-skill]
                     [forge.entity.state.npc-dead :npc-dead]
                     [forge.entity.state.npc-idle :npc-idle]
                     [forge.entity.state.npc-moving :npc-moving]
                     [forge.entity.state.npc-sleeping :npc-sleeping]
                     [forge.entity.state.player-dead :player-dead]
                     [forge.entity.state.player-idle :player-idle]
                     [forge.entity.state.player-item-on-cursor :player-item-on-cursor]
                     [forge.entity.state.player-moving :player-moving]
                     [forge.entity.state.stunned :stunned]]]
  (install-component entity-state ns-sym k))
