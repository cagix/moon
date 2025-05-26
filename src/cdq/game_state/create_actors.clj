(ns cdq.game-state.create-actors
  (:require [cdq.g :as g]
            [cdq.game-state :as game-state]
            [cdq.ui.dev-menu]
            [cdq.ui.action-bar]
            [cdq.ui.hp-mana-bar]
            [cdq.ui.windows]
            [cdq.ui.entity-info]
            [cdq.ui.inventory]
            [cdq.ui.player-state-draw]
            [cdq.ui.message]))

(defn create-actors [ctx]
  [(cdq.ui.dev-menu/create ctx)
   (cdq.ui.action-bar/create :id :action-bar)
   (cdq.ui.hp-mana-bar/create [(/ (g/ui-viewport-width ctx) 2)
                               80 ; action-bar-icon-size
                               ]
                              ctx)
   (cdq.ui.windows/create :id :windows
                          :actors [(cdq.ui.entity-info/create [(g/ui-viewport-width ctx) 0])
                                   (cdq.ui.inventory/create ctx
                                                            :id :inventory-window
                                                            :position [(g/ui-viewport-width ctx)
                                                                       (g/ui-viewport-height ctx)])])
   (cdq.ui.player-state-draw/create)
   (cdq.ui.message/create :name "player-message")])
