(ns cdq.create.actors
  (:require [cdq.ui.action-bar]
            [cdq.ui.hp-mana-bar]
            [cdq.ui.windows]
            [cdq.ui.entity-info]
            [cdq.ui.inventory]
            [cdq.ui.player-state-draw]
            [cdq.ui.message]))

(defprotocol DevMenuActor
  (create-dev-menu [ctx]))

(defn create-actors [{:keys [ctx/ui-viewport]
                      :as ctx}]
  [(create-dev-menu ctx)
   (cdq.ui.action-bar/create :id :action-bar)
   (cdq.ui.hp-mana-bar/create [(/ (:width ui-viewport) 2)
                               80 ; action-bar-icon-size
                               ]
                              ctx)
   (cdq.ui.windows/create :id :windows
                          :actors [(cdq.ui.entity-info/create [(:width ui-viewport) 0])
                                   (cdq.ui.inventory/create ctx
                                                            :id :inventory-window
                                                            :position [(:width ui-viewport)
                                                                       (:height ui-viewport)])])
   (cdq.ui.player-state-draw/create)
   (cdq.ui.message/create :name "player-message")])
