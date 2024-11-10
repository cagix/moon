(ns moon.components
  (:require [moon.component :as component]
            [moon.entity :as entity]
            [moon.entity.fsm :as fsm]
            [moon.player :as player]))

(def effect
  {:required [#'component/applicable?
              #'component/handle]
   :optional [#'component/info
              #'component/useful?
              #'component/render]})

(def fsm
  {:required [#'component/create]})

(def entity
  {:optional [#'component/info
              #'component/handle
              #'entity/->v
              #'entity/create
              #'entity/destroy
              #'entity/tick
              #'entity/render-below
              #'entity/render
              #'entity/render-above
              #'entity/render-info]})

(def entity-state
  (merge-with concat
              entity
              {:optional [#'fsm/enter
                          #'fsm/exit
                          #'fsm/cursor
                          #'player/pause-game?
                          #'player/manual-tick
                          #'player/clicked-inventory-cell
                          #'player/clicked-skillmenu-skill
                          #'player/draw-gui-view]}))
