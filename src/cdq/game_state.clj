(ns cdq.game-state
  (:require [cdq.cell :as cell]
            [cdq.content-grid :as content-grid]
            [cdq.ctx :as ctx]
            [cdq.entity :as entity]
            [cdq.state :as state]
            [cdq.g :as g]
            [cdq.grid :as grid]
            [cdq.grid-impl :as grid-impl]
            [cdq.grid2d :as g2d]
            [cdq.potential-fields.movement :as potential-fields.movement]
            [cdq.raycaster :as raycaster]
            [cdq.timer :as timer]
            [cdq.ui.action-bar :as action-bar]
            [cdq.ui.inventory :as inventory-window]
            [cdq.ui.error-window :as error-window]
            [gdl.application]
            [gdl.tiled :as tiled]
            [gdl.utils :as utils]))

(extend-type gdl.application.Context
  g/Context
  (context-entity-add! [{:keys [ctx/entity-ids
                                ctx/content-grid
                                ctx/grid]}
                        eid]
    (let [id (entity/id @eid)]
      (assert (number? id))
      (swap! entity-ids assoc id eid))
    (content-grid/add-entity! content-grid eid)
    ; https://github.com/damn/core/issues/58
    ;(assert (valid-position? grid @eid)) ; TODO deactivate because projectile no left-bottom remove that field or update properly for all
    (grid/add-entity! grid eid))

  (context-entity-remove! [{:keys [ctx/entity-ids
                                   ctx/grid]}
                           eid]
    (let [id (entity/id @eid)]
      (assert (contains? @entity-ids id))
      (swap! entity-ids dissoc id))
    (content-grid/remove-entity! eid)
    (grid/remove-entity! grid eid))

  (context-entity-moved! [{:keys [ctx/content-grid
                                  ctx/grid]}
                          eid]
    (content-grid/position-changed! content-grid eid)
    (grid/position-changed! grid eid)))

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
  (g/spawn-creature! ctx
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

(extend-type gdl.application.Context
  g/StageActors
  (open-error-window! [ctx throwable]
    (g/add-actor! ctx (error-window/create throwable)))

  (selected-skill [ctx]
    (action-bar/selected-skill (g/get-actor ctx :action-bar))))

(extend-type gdl.application.Context
  g/Raycaster
  (ray-blocked? [{:keys [ctx/raycaster]} start end]
    (raycaster/blocked? raycaster
                        start
                        end))

  (path-blocked? [{:keys [ctx/raycaster]} start end width]
    (raycaster/path-blocked? raycaster
                             start
                             end
                             width)))

(extend-type gdl.application.Context
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

(extend-type gdl.application.Context
  cdq.g/Grid
  (grid-cell [{:keys [ctx/grid]} position]
    (grid/cell grid position))

  (point->entities [{:keys [ctx/grid]} position]
    (grid/point->entities grid position))

  (valid-position? [{:keys [ctx/grid]} new-body]
    (grid/valid-position? grid new-body))

  (circle->cells [{:keys [ctx/grid]} circle]
    (grid/circle->cells grid circle))

  (circle->entities [{:keys [ctx/grid]} circle]
    (grid/circle->entities grid circle))

  (nearest-enemy-distance [{:keys [ctx/grid]} entity]
    (cell/nearest-entity-distance @(grid/cell grid (mapv int (entity/position entity)))
                                  (entity/enemy entity)))

  (nearest-enemy [{:keys [ctx/grid]} entity]
    (cell/nearest-entity @(grid/cell grid (mapv int (entity/position entity)))
                         (entity/enemy entity)))

  (potential-field-find-direction [{:keys [ctx/grid]} eid]
    (potential-fields.movement/find-direction grid eid)))

(defprotocol StageActors
  (create-actors [_]))

(defn create! [ctx]
  (g/reset-actors! ctx (create-actors ctx))
  (let [{:keys [tiled-map
                start-position]} ((g/config ctx :world-fn) ctx)
        grid (grid-impl/create tiled-map)
        ctx (merge ctx
                   {:ctx/tiled-map tiled-map
                    :ctx/elapsed-time 0
                    :ctx/grid grid
                    :ctx/raycaster (raycaster/create grid)
                    :ctx/content-grid (content-grid/create tiled-map (g/config ctx :content-grid-cell-size))
                    :ctx/explored-tile-corners (atom (g2d/create-grid (tiled/tm-width  tiled-map)
                                                                      (tiled/tm-height tiled-map)
                                                                      (constantly false)))
                    :ctx/id-counter (atom 0)
                    :ctx/entity-ids (atom {})
                    :ctx/potential-field-cache (atom nil)})
        ctx (assoc ctx :ctx/player-eid (spawn-player-entity ctx start-position))]
    (g/handle-txs! ctx (spawn-enemies tiled-map))
    ctx))
