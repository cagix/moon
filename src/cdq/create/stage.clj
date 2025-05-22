(ns cdq.create.stage
  (:require [cdq.ui.action-bar]
            [cdq.ui.entity-info]
            [cdq.ui.inventory]
            [cdq.ui.hp-mana-bar]
            [cdq.ui.dev-menu]
            [cdq.ui.player-state-draw]
            [cdq.ui.windows]
            [cdq.ui.message]
            [gdl.ui :as ui]))

(defn- create-actors [{:keys [ctx/ui-viewport] :as ctx}]
  [(cdq.ui.dev-menu/create ctx)
   (cdq.ui.action-bar/create :id :action-bar)
   (cdq.ui.hp-mana-bar/create [(/ (:width ui-viewport) 2)
                               80 ; action-bar-icon-size
                               ]
                              ctx)
   (cdq.ui.windows/create :id :windows
                          :actors [(cdq.ui.entity-info/create [(:width ui-viewport) 0])
                                   (cdq.ui.inventory/create ctx
                                                            :id :inventory-window
                                                            :position [(:width  ui-viewport)
                                                                       (:height ui-viewport)])])
   (cdq.ui.player-state-draw/create)
   (cdq.ui.message/create :name "player-message")])

(defn do!
  [{:keys [ctx/stage]
    :as ctx}]
  (ui/clear! stage)
  (run! #(ui/add! stage %) (create-actors ctx))
  nil)
