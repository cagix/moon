(ns cdq.application.create.game-state
  (:require [cdq.ctx :as ctx]
            [cdq.ctx.spawn-enemies :as spawn-enemies]
            [cdq.content-grid :as content-grid]
            [cdq.grid :as grid]
            [cdq.grid2d :as g2d]
            [cdq.raycaster :as raycaster]
            [cdq.state :as state]
            [cdq.ui.action-bar :as action-bar]
            [cdq.ui.entity-info]
            [cdq.ui.inventory :as inventory-window]
            [cdq.ui.hp-mana-bar]
            [cdq.ui.dev-menu]
            [cdq.ui.player-state-draw]
            [cdq.ui.windows]
            [cdq.ui.message]
            [cdq.utils :refer [bind-root
                               handle-txs!
                               tile->middle]]
            [gdl.input :as input]
            [gdl.tiled :as tiled]
            [gdl.ui :as ui]))

(defn- player-entity-props [start-position]
  {:position (tile->middle start-position)
   :creature-id (:creature-id ctx/player-entity-config)
   :components {:entity/fsm {:fsm :fsms/player
                             :initial-state :player-idle}
                :entity/faction :good
                :entity/player? {:state-changed! (fn [new-state-obj]
                                                   (when-let [cursor (state/cursor new-state-obj)]
                                                     [[:tx/set-cursor cursor]]))
                                 :skill-added! (fn [skill]
                                                 (-> ctx/stage
                                                     :action-bar
                                                     (action-bar/add-skill! skill)))
                                 :skill-removed! (fn [skill]
                                                   (-> ctx/stage
                                                       :action-bar
                                                       (action-bar/remove-skill! skill)))
                                 :item-set! (fn [inventory-cell item]
                                              (-> ctx/stage
                                                  :windows
                                                  :inventory-window
                                                  (inventory-window/set-item! inventory-cell item)))
                                 :item-removed! (fn [inventory-cell]
                                                  (-> ctx/stage
                                                      :windows
                                                      :inventory-window
                                                      (inventory-window/remove-item! inventory-cell)))}
                :entity/free-skill-points (:free-skill-points ctx/player-entity-config)
                :entity/clickable {:type :clickable/player}
                :entity/click-distance-tiles (:click-distance-tiles ctx/player-entity-config)}})

(defn- spawn-player [start-position]
  [[:tx/spawn-creature (player-entity-props start-position)]])

(declare reset-game!)

(defn- reset-game! [world-fn]
  (bind-root #'ctx/elapsed-time 0)
  (bind-root #'ctx/stage (ui/stage (:java-object ctx/ui-viewport)
                                   (:java-object ctx/batch)
                                   [(cdq.ui.dev-menu/create #'reset-game!)
                                    (action-bar/create :id :action-bar)
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
  (input/set-processor! ctx/stage)
  (let [{:keys [tiled-map start-position]} ((requiring-resolve world-fn))
        width  (tiled/tm-width  tiled-map)
        height (tiled/tm-height tiled-map)]
    (bind-root #'ctx/tiled-map tiled-map)
    (bind-root #'ctx/grid (grid/create tiled-map))
    (bind-root #'ctx/raycaster (raycaster/create ctx/grid))
    (bind-root #'ctx/content-grid (content-grid/create {:cell-size (:content-grid-cells-size ctx/config)
                                                        :width  width
                                                        :height height}))
    (bind-root #'ctx/explored-tile-corners (atom (g2d/create-grid width
                                                                  height
                                                                  (constantly false))))
    (bind-root #'ctx/id-counter (atom 0))
    (bind-root #'ctx/entity-ids (atom {}))
    (bind-root #'ctx/potential-field-cache (atom nil))
    (spawn-enemies/do! {:ctx/tiled-map ctx/tiled-map})
    (handle-txs! (spawn-player start-position))))

(defn do! []
  (reset-game! (:tiled-map ctx/config)))
