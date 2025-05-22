(ns cdq.g.game-state
  (:require [cdq.ctx :as ctx]
            [cdq.content-grid]
            [cdq.g :as g]
            [cdq.state :as state]
            [cdq.tx.spawn-creature]
            [cdq.grid]
            [cdq.grid2d :as g2d]
            [cdq.raycaster]
            [cdq.ui.action-bar :as action-bar]
            [cdq.ui.dev-menu]
            [cdq.ui.entity-info]
            [cdq.ui.inventory :as inventory-window]
            [cdq.ui.hp-mana-bar]
            [cdq.ui.player-state-draw]
            [cdq.ui.windows]
            [cdq.ui.message]
            [cdq.utils :as utils]
            [gdl.tiled :as tiled]
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

(defn reset-stage! [stage actors]
  (ui/clear! stage)
  (run! #(ui/add! stage %) actors))

(defn- player-entity-props [start-position {:keys [creature-id
                                                   free-skill-points
                                                   click-distance-tiles]}]
  {:position start-position
   :creature-id creature-id
   :components {:entity/fsm {:fsm :fsms/player
                             :initial-state :player-idle}
                :entity/faction :good
                :entity/player? {:state-changed! (fn [new-state-obj]
                                                   (when-let [cursor (state/cursor new-state-obj)]
                                                     [[:tx/set-cursor cursor]]))
                                 :skill-added! (fn [{:keys [ctx/stage]} skill]
                                                 (-> stage
                                                     :action-bar
                                                     (action-bar/add-skill! skill)))
                                 :skill-removed! (fn [{:keys [ctx/stage]} skill]
                                                   (-> stage
                                                       :action-bar
                                                       (action-bar/remove-skill! skill)))
                                 :item-set! (fn [{:keys [ctx/stage]} inventory-cell item]
                                              (-> stage
                                                  :windows
                                                  :inventory-window
                                                  (inventory-window/set-item! inventory-cell item)))
                                 :item-removed! (fn [{:keys [ctx/stage]} inventory-cell]
                                                  (-> stage
                                                      :windows
                                                      :inventory-window
                                                      (inventory-window/remove-item! inventory-cell)))}
                :entity/free-skill-points free-skill-points
                :entity/clickable {:type :clickable/player}
                :entity/click-distance-tiles click-distance-tiles}})

(defn- spawn-player-entity [ctx start-position]
  (cdq.tx.spawn-creature/do! ctx
                             (player-entity-props (utils/tile->middle start-position)
                                                  ctx/player-entity-config)))

(defn- spawn-enemies* [tiled-map]
  (for [props (for [[position creature-id] (tiled/positions-with-property tiled-map :creatures :id)]
                {:position position
                 :creature-id (keyword creature-id)
                 :components {:entity/fsm {:fsm :fsms/npc
                                           :initial-state :npc-sleeping}
                              :entity/faction :evil}})]
    [:tx/spawn-creature (update props :position utils/tile->middle)]))

(defn- spawn-enemies! [{:keys [ctx/tiled-map] :as ctx}]
  (g/handle-txs! ctx (spawn-enemies* tiled-map)))

(defn- create-game-state [{:keys [ctx/config] :as ctx}]
  (reset-stage! (:ctx/stage ctx)
                (create-actors ctx))
  (let [{:keys [tiled-map
                start-position]} ((requiring-resolve (:world-fn config)) ctx)
        grid (cdq.grid/create tiled-map)
        ctx (merge ctx
                   {:ctx/tiled-map tiled-map
                    :ctx/elapsed-time 0
                    :ctx/grid grid
                    :ctx/raycaster (cdq.raycaster/create grid)
                    :ctx/content-grid (cdq.content-grid/create tiled-map (:content-grid-cell-size config))
                    :ctx/explored-tile-corners (atom (g2d/create-grid (tiled/tm-width  tiled-map)
                                                                      (tiled/tm-height tiled-map)
                                                                      (constantly false)))
                    :ctx/id-counter (atom 0)
                    :ctx/entity-ids (atom {})
                    :ctx/potential-field-cache (atom nil)})
        ctx (assoc ctx :ctx/player-eid (spawn-player-entity ctx start-position))]
    (spawn-enemies! ctx)
    ctx))

(extend-type cdq.g.Game
  g/GameState
  (reset-game-state! [ctx]
    (create-game-state ctx)))
