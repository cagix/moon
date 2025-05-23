(ns cdq.g.game-state.stage
  (:require [cdq.g :as g]
            [cdq.ui.action-bar :as action-bar]
            [cdq.ui.dev-menu]
            [cdq.ui.entity-info]
            [cdq.ui.inventory]
            [cdq.ui.hp-mana-bar]
            [cdq.ui.player-state-draw]
            [cdq.ui.windows]
            [cdq.ui.message]
            [gdl.ui :as ui]))

(defn- create-actors [{:keys [ctx/ui-viewport] :as ctx}]
  [(cdq.ui.dev-menu/create ctx)
   (action-bar/create :id :action-bar)
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

(defn reset [{:keys [ctx/stage] :as ctx}]
  (ui/clear! stage)
  (run! #(ui/add! stage %) (create-actors ctx)))

(extend-type cdq.g.Game
  g/Stage
  (mouseover-actor [{:keys [ctx/stage] :as ctx}]
    (ui/hit stage (g/ui-mouse-position ctx)))

  (selected-skill [{:keys [ctx/stage]}]
    (action-bar/selected-skill (:action-bar stage))))
