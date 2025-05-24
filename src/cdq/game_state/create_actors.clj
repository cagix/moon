(ns cdq.game-state.create-actors
  (:require [cdq.game-state :as game-state]
            [cdq.ui.action-bar]
            [cdq.ui.dev-menu]
            [cdq.ui.entity-info]
            [cdq.ui.inventory]
            [cdq.ui.hp-mana-bar]
            [cdq.ui.message]
            [cdq.ui.player-state-draw]
            [cdq.ui.windows]
            [gdl.application]
            [gdl.c :as c]))

(extend-type gdl.application.Context
  game-state/StageActors
  (create-actors [ctx]
    [(cdq.ui.dev-menu/create ctx)
     (cdq.ui.action-bar/create :id :action-bar)
     (cdq.ui.hp-mana-bar/create [(/ (c/ui-viewport-width ctx) 2)
                                 80 ; action-bar-icon-size
                                 ]
                                ctx)
     (cdq.ui.windows/create :id :windows
                            :actors [(cdq.ui.entity-info/create [(c/ui-viewport-width ctx) 0])
                                     (cdq.ui.inventory/create ctx
                                                              :id :inventory-window
                                                              :position [(c/ui-viewport-width ctx)
                                                                         (c/ui-viewport-height ctx)])])
     (cdq.ui.player-state-draw/create)
     (cdq.ui.message/create :name "player-message")]))
