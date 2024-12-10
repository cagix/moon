
#_(def effect {:required [#'effect/applicable?
                          #'effect/handle]
               :optional [#'world.update/useful?
                          #'active-skill/render]})

#_(def entity
    {:optional [#'entity/->v
                #'entity/create
                #'world.update/destroy
                #'world.update/tick
                #'render/render-below
                #'render/render-default
                #'render/render-above
                #'render/render-info]})

#_(def entity-state
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

