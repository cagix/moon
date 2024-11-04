(ns moon.components
  (:require [moon.component :as component]
            [moon.entity :as entity]))

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
              {:optional [#'entity/enter
                          #'entity/exit
                          #'entity/player-enter
                          #'entity/pause-game?
                          #'entity/manual-tick
                          #'entity/clicked-inventory-cell
                          #'entity/clicked-skillmenu-skill
                          #'entity/draw-gui-view]}))

(def tx
  {:required [#'component/handle]})

(def widget
  {:required [#'component/create]
   :optional [#'component/handle]})
