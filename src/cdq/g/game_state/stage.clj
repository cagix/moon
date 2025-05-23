(ns cdq.g.game-state.stage
  (:require [cdq.g :as g]
            [cdq.ui.action-bar :as action-bar]
            [cdq.ui.dev-menu]
            [cdq.ui.entity-info]
            [cdq.ui.error-window :as error-window]
            [cdq.ui.inventory]
            [cdq.ui.hp-mana-bar]
            [cdq.ui.player-state-draw]
            [cdq.ui.windows]
            [cdq.ui.message]))

(defn- create-actors [ctx]
  [(cdq.ui.dev-menu/create ctx)
   (action-bar/create :id :action-bar)
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

(defn reset [ctx]
  (g/reset-actors! ctx (create-actors ctx)))

(extend-type cdq.g.Game
  g/StageActors
  (open-error-window! [ctx throwable]
    (g/add-actor! ctx (error-window/create throwable)))

  (selected-skill [ctx]
    (action-bar/selected-skill (g/get-actor ctx :action-bar))))

; TODO get-inventory-window
; get-action-bar, etc. ...
; use get-actor internally

; => game context API hides internal gdl api
; => gdl api in different namespace ?

; show-modal here
; tx player message, etc.
; cdq.tx.toggle-inventory-visible
