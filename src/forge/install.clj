(ns forge.install
  (:require [clojure.utils :refer [install install-component]]
            [forge.app :as component]
            [forge.effect :as effect]
            [forge.entity :as entity]
            [forge.entity.state :as state]))

(install "forge"
         {:optional [#'component/create
                     #'component/destroy
                     #'component/render
                     #'component/resize]}
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
         {:required [#'effect/applicable?
                     #'effect/handle]
          :optional [#'effect/useful?
                     #'effect/render-effect]}
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
  {:optional [#'entity/->v
              #'entity/create
              #'entity/destroy
              #'entity/tick
              #'entity/render-below
              #'entity/render-default
              #'entity/render-above
              #'entity/render-info]})

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
              {:optional [#'state/enter
                          #'state/exit
                          #'state/cursor
                          #'state/pause-game?
                          #'state/manual-tick
                          #'state/clicked-inventory-cell
                          #'state/clicked-skillmenu-skill
                          #'state/draw-gui-view]}))

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
