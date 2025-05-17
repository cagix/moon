(ns cdq.stage
  (:require [cdq.ctx :as ctx]
            [cdq.ui.action-bar]
            [cdq.ui.hp-mana-bar]
            [cdq.ui.player-state-draw]
            [cdq.ui.message]
            [cdq.ui.windows]
            [gdl.ui.stage :as stage]))

(defn show-message! [stage text]
  (cdq.ui.message/show! (stage/root stage) text))

(defn create [{:keys [dev-menu]}]
  (stage/create (:java-object ctx/ui-viewport)
                (:java-object ctx/batch)
                [dev-menu
                 (cdq.ui.action-bar/create)
                 (cdq.ui.hp-mana-bar/create [(/ (:width ctx/ui-viewport) 2)
                                             80 ; action-bar-icon-size
                                             ])
                 (cdq.ui.windows/create)
                 (cdq.ui.player-state-draw/create)
                 (cdq.ui.message/create)]))
