(ns cdq.g.game-state
  (:require [cdq.ctx :as ctx]
            [cdq.cell :as cell]
            [cdq.content-grid]
            [cdq.entity :as entity]
            [cdq.g :as g]
            [cdq.g.game-state.stage :as stage]
            [cdq.grid :as grid]
            [cdq.grid2d :as g2d]
            [cdq.state :as state]
            [cdq.timer :as timer]
            [cdq.tile-color-setter :as tile-color-setter]
            [cdq.tx.spawn-creature]
            [cdq.potential-field.movement :as potential-field]
            [cdq.raycaster]
            [cdq.ui.action-bar :as action-bar]
            [cdq.ui.inventory :as inventory-window]
            [cdq.utils :as utils]
            [gdl.tiled :as tiled]))

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
                                 :skill-added! (fn [ctx skill]
                                                 (action-bar/add-skill! (g/get-actor ctx :action-bar)
                                                                        skill))
                                 :skill-removed! (fn [ctx skill]
                                                   (action-bar/remove-skill! (g/get-actor ctx :action-bar)
                                                                             skill))
                                 :item-set! (fn [ctx inventory-cell item]
                                              (-> (g/get-actor ctx :windows)
                                                  :inventory-window
                                                  (inventory-window/set-item! inventory-cell item)))
                                 :item-removed! (fn [ctx inventory-cell]
                                                  (-> (g/get-actor ctx :windows)
                                                      :inventory-window
                                                      (inventory-window/remove-item! inventory-cell)))}
                :entity/free-skill-points free-skill-points
                :entity/clickable {:type :clickable/player}
                :entity/click-distance-tiles click-distance-tiles}})

(defn- spawn-player-entity [ctx start-position]
  (cdq.tx.spawn-creature/do! ctx
                             (player-entity-props (utils/tile->middle start-position)
                                                  ctx/player-entity-config)))

(defn- spawn-enemies [tiled-map]
  (for [props (for [[position creature-id] (tiled/positions-with-property tiled-map :creatures :id)]
                {:position position
                 :creature-id (keyword creature-id)
                 :components {:entity/fsm {:fsm :fsms/npc
                                           :initial-state :npc-sleeping}
                              :entity/faction :evil}})]
    [:tx/spawn-creature (update props :position utils/tile->middle)]))

(defn- create-game-state [ctx]
  (stage/reset ctx)
  (let [{:keys [tiled-map
                start-position]} ((requiring-resolve (g/config ctx :world-fn)) ctx)
        grid (grid/create tiled-map)
        ctx (merge ctx
                   {:ctx/tiled-map tiled-map
                    :ctx/elapsed-time 0
                    :ctx/grid grid
                    :ctx/raycaster (cdq.raycaster/create grid)
                    :ctx/content-grid (cdq.content-grid/create tiled-map (g/config ctx :content-grid-cell-size))
                    :ctx/explored-tile-corners (atom (g2d/create-grid (tiled/tm-width  tiled-map)
                                                                      (tiled/tm-height tiled-map)
                                                                      (constantly false)))
                    :ctx/id-counter (atom 0)
                    :ctx/entity-ids (atom {})
                    :ctx/potential-field-cache (atom nil)})
        ctx (assoc ctx :ctx/player-eid (spawn-player-entity ctx start-position))]
    (g/handle-txs! ctx (spawn-enemies tiled-map))
    ctx))

(extend-type cdq.g.Game
  g/GameState
  (reset-game-state! [ctx]
    (create-game-state ctx))

  g/World
  (draw-world-map! [{:keys [ctx/tiled-map
                            ctx/raycaster
                            ctx/explored-tile-corners]
                     :as ctx}]
    (g/draw-tiled-map! ctx
                       tiled-map
                       (tile-color-setter/create raycaster
                                                 explored-tile-corners
                                                 (g/camera-position ctx))))

  g/Time
  (elapsed-time [{:keys [ctx/elapsed-time]}]
    elapsed-time)

  (create-timer [{:keys [ctx/elapsed-time]} duration]
    (timer/create elapsed-time duration))

  (timer-stopped? [{:keys [ctx/elapsed-time]} timer]
    (timer/stopped? elapsed-time timer))

  (reset-timer [{:keys [ctx/elapsed-time]} timer]
    (timer/reset elapsed-time timer))

  (timer-ratio [{:keys [ctx/elapsed-time]} timer]
    (timer/ratio elapsed-time timer)))

(extend-type cdq.g.Game
  cdq.g/Grid
  (valid-position? [{:keys [ctx/grid]} new-body]
    (grid/valid-position? grid new-body))

  (circle->entities [{:keys [ctx/grid]} circle]
    (grid/circle->entities grid circle))

  (nearest-enemy-distance [{:keys [ctx/grid]} entity]
    (cell/nearest-entity-distance @(grid (mapv int (:position entity)))
                                  (entity/enemy entity)))

  (nearest-enemy [{:keys [ctx/grid]} entity]
    (cell/nearest-entity @(grid (mapv int (:position entity)))
                         (entity/enemy entity)))

  (potential-field-find-direction [{:keys [ctx/grid]} eid]
    (potential-field/find-direction grid eid)))
