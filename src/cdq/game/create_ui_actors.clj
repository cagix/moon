(ns cdq.game.create-ui-actors
  (:require [cdq.ctx :as ctx]
            cdq.ui.dev-menu
            cdq.ui.action-bar
            cdq.ui.hp-mana-bar
            cdq.ui.windows.entity-info
            cdq.ui.windows.inventory
            cdq.ui.player-state-draw
            cdq.ui.message))

(def state->clicked-inventory-cell)

(comment

 ; items then have 2x pretty-name
 #_(.setText (.getTitleLabel window)
             (info-text [:property/pretty-name (:property/pretty-name entity)])
             "Entity Info")
 )

(def disallowed-keys [:entity/skills
                      #_:entity/fsm
                      :entity/faction
                      :active-skill])

; TODO details how the text looks move to info
; only for :
; * skill
; * entity -> all sub-types
; * item
; => can test separately !?

(defn- ->label-text [entity ctx]
  ; don't use select-keys as it loses Entity record type
  (ctx/info-text ctx (apply dissoc entity disallowed-keys)))

(def state->draw-gui-view)

(defn do! [ctx]
  [(cdq.ui.dev-menu/create ctx ; graphics db
                           {:reset-game-state-fn (requiring-resolve (:reset-game-state! (:ctx/config ctx)))
                            :world-fns [['cdq.level.from-tmx/create
                                         {:tmx-file "maps/vampire.tmx"
                                          :start-position [32 71]}]
                                        ['cdq.level.uf-caves/create
                                         {:tile-size 48
                                          :texture "maps/uf_terrain.png"
                                          :spawn-rate 0.02
                                          :scaling 3
                                          :cave-size 200
                                          :cave-style :wide}]
                                        ['cdq.level.modules/create
                                         {:world/map-size 5,
                                          :world/max-area-level 3,
                                          :world/spawn-rate 0.05}]]
                            ;icons, etc. , components ....
                            :info "[W][A][S][D] - Move\n[I] - Inventory window\n[E] - Entity Info window\n[-]/[=] - Zoom\n[P]/[SPACE] - Unpause"})
    (cdq.ui.action-bar/create {:id :action-bar}) ; padding.... !, etc.

    ; graphics
    (cdq.ui.hp-mana-bar/create ctx
                               {:rahmen-file "images/rahmen.png"
                                :rahmenw 150
                                :rahmenh 26
                                :hpcontent-file "images/hp.png"
                                :manacontent-file "images/mana.png"
                                :y-mana 80}) ; action-bar-icon-size

    {:actor/type :actor.type/group
     :id :windows
     :actors [(cdq.ui.windows.entity-info/create ctx {:y 0
                                                      :->label-text ->label-text
                                                      }) ; graphics only
              (cdq.ui.windows.inventory/create ctx ; graphics only
               {:title "Inventory"
                :id :inventory-window
                :visible? false
                :clicked-cell-fn (fn [cell]
                                   (fn [{:keys [ctx/player-eid] :as ctx}]
                                     (ctx/handle-txs!
                                      ctx
                                      (when-let [f (state->clicked-inventory-cell (:state (:entity/fsm @player-eid)))]
                                        (f player-eid cell)))))})]}
    (cdq.ui.player-state-draw/create state->draw-gui-view)
    (cdq.ui.message/create {:duration-seconds 0.5
                            :name "player-message"})])
