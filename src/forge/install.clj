(ns forge.install
  (:require [anvil.app :as app]
            [anvil.screen :as screen]
            [clojure.component :as component]
            [clojure.utils :refer [install install-component]]

            forge.schemas
            forge.info
            forge.mapgen.generate
            forge.mapgen.uf-caves))

(def screen {:optional [#'app/actors
                        #'screen/enter
                        #'screen/exit
                        #'screen/dispose
                        #'screen/render]})

(install "forge"
         screen
         (map vector [:screens/stage
                      :screens/editor
                      :screens/main-menu
                      :screens/map-editor
                      :screens/minimap
                      :screens/world]))

(install "forge"
         {:required [#'component/applicable?
                     #'component/handle]
          :optional [#'component/useful?
                     #'component/render-effect]}
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
  {:optional [#'component/->v
              #'component/create
              #'component/destroy
              #'component/tick
              #'component/render-below
              #'component/render-default
              #'component/render-above
              #'component/render-info]})

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
              {:optional [#'component/enter
                          #'component/exit
                          #'component/cursor
                          #'component/pause-game?
                          #'component/manual-tick
                          #'component/clicked-inventory-cell
                          #'component/clicked-skillmenu-skill
                          #'component/draw-gui-view]}))

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
