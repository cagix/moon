(ns forge.install
  (:require [anvil.app :as app]
            [anvil.effect :as effect]
            [anvil.entity :as entity]
            [forge.entity.state.active-skill :as active-skill]
            [anvil.fsm :as fsm]
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

#_(def effect {:required [#'effect/applicable?
                          #'effect/handle]
               :optional [#'world.update/useful?
                          #'active-skill/render]})

(def entity
  {:optional [#'entity/->v
              #'entity/create
              #'world.update/destroy
              #'world.update/tick
              #'render/render-below
              #'render/render-default
              #'render/render-above
              #'render/render-info]})

(def entity-state
  (merge-with concat
              entity
              {:optional [#'fsm/enter
                          #'fsm/exit
                          #'fsm/cursor
                          #'world.update/pause-game?
                          #'world.update/manual-tick
                          #'forge.world.create/clicked-inventory-cell
                          #'clicked-skillmenu-skill
                          #'draw-gui-view]}))

; npc moving is basically a performance optimization so npcs do not have to check
; usable skills every frame
; also prevents fast twitching around changing directions every frame

(doseq [[ns-sym k] '[[forge.entity.state.active-skill :active-skill]
                     [forge.entity.state.npc-dead :npc-dead]]]
  (install-component entity-state ns-sym k))
