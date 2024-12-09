(ns forge.install
  (:require [anvil.app :as app]
            [anvil.effect :as effect]
            [anvil.entity :as entity]
            [forge.entity.state.active-skill :as active-skill]
            [forge.entity.state.npc-idle :as npc-idle]
            [anvil.fsm :as fsm]
            [anvil.inventory :as inventory]
            [forge.world.render :as render]
            [anvil.screen :as screen]
            [clojure.utils :refer [install install-component]]
            forge.schemas
            forge.info
            forge.mapgen.generate
            forge.mapgen.uf-caves
            [forge.ui.skill-window :refer [clicked-skillmenu-skill]]
            [forge.world.create :refer [draw-gui-view]]
            [forge.world.update :as world.update]))

(install "forge"
         {:required [#'effect/applicable?
                     #'effect/handle]
          :optional [#'npc-idle/useful?
                     #'active-skill/render]}
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
              #'world.update/destroy
              #'world.update/tick
              #'render/render-below
              #'render/render-default
              #'render/render-above
              #'render/render-info]})

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
              {:optional [#'fsm/enter
                          #'fsm/exit
                          #'fsm/cursor
                          #'world.update/pause-game?
                          #'world.update/manual-tick
                          #'inventory/clicked-inventory-cell
                          #'clicked-skillmenu-skill
                          #'draw-gui-view]}))

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
