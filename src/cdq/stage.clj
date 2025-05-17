(ns cdq.stage
  (:require [cdq.ctx :as ctx]
            [cdq.ui.action-bar]
            [cdq.ui.hp-mana-bar]
            [cdq.ui.inventory]
            [cdq.ui.player-state-draw]
            [cdq.ui.message]
            [cdq.ui.windows]
            [gdl.ui.stage :as stage]))

(defn set-item! [stage cell item]
  (cdq.ui.inventory/set-item! stage cell item))

(defn remove-item! [stage cell]
  (cdq.ui.inventory/remove-item! stage cell))

(defn selected-skill [stage]
  (cdq.ui.action-bar/selected-skill stage))

(defn add-skill! [stage skill]
  (cdq.ui.action-bar/add-skill! stage skill))

(defn remove-skill! [stage skill]
  (cdq.ui.action-bar/remove-skill! stage skill))

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
