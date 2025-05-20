(ns cdq.ctx.init-stage
  (:require [cdq.ctx :as ctx]
            [cdq.ui.action-bar]
            [cdq.ui.entity-info]
            [cdq.ui.inventory ]
            [cdq.ui.hp-mana-bar]
            [cdq.ui.dev-menu]
            [cdq.ui.player-state-draw]
            [cdq.ui.windows]
            [cdq.ui.message]
            [cdq.utils :refer [bind-root]]
            [gdl.input :as input]
            [gdl.ui :as ui]))

(defn do! []
  (bind-root #'ctx/stage (ui/stage (:java-object ctx/ui-viewport)
                                   (:java-object ctx/batch)
                                   [(cdq.ui.dev-menu/create)
                                    (cdq.ui.action-bar/create :id :action-bar)
                                    (cdq.ui.hp-mana-bar/create [(/ (:width ctx/ui-viewport) 2)
                                                                80 ; action-bar-icon-size
                                                                ])
                                    (cdq.ui.windows/create :id :windows
                                                           :actors [(cdq.ui.entity-info/create [(:width ctx/ui-viewport) 0])
                                                                    (cdq.ui.inventory/create :id :inventory-window
                                                                                             :position [(:width  ctx/ui-viewport)
                                                                                                        (:height ctx/ui-viewport)])])
                                    (cdq.ui.player-state-draw/create)
                                    (cdq.ui.message/create :name "player-message")]))
  (input/set-processor! ctx/stage))
