(ns cdq.create.game-state
  (:require [cdq.application]
            [cdq.cell :as cell]
            [cdq.content-grid :as content-grid]
            [cdq.entity :as entity]
            [cdq.game]
            [cdq.graphics :as graphics]
            [cdq.ctx.spawn-entity]
            [cdq.ctx.spawn-creature]
            [cdq.grid :as grid]
            [cdq.grid-impl :as grid-impl]
            [cdq.grid2d :as g2d]
            [cdq.potential-fields.movement :as potential-fields.movement]
            [cdq.raycaster :as raycaster]
            [cdq.state :as state]
            [cdq.ui.action-bar :as action-bar]
            [cdq.ui.inventory :as inventory-window]
            [cdq.world :as world]
            [gdl.ctx :as ctx]
            [gdl.tiled :as tiled]
            [gdl.ui.stage :as stage]
            [gdl.utils :as utils])
  (:import (cdq.application Context)))

(defn- add-skill! [ctx skill]
  (-> ctx :ctx/stage :action-bar (action-bar/add-skill! skill)))

(defn- remove-skill! [ctx skill]
  (-> ctx :ctx/stage :action-bar (action-bar/remove-skill! skill)))

(defn- set-item! [ctx inventory-cell item]
  (-> ctx :ctx/stage :windows :inventory-window (inventory-window/set-item! inventory-cell item)))

(defn- remove-item! [ctx inventory-cell]
  (-> ctx :ctx/stage :windows :inventory-window (inventory-window/remove-item! inventory-cell)))

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
                                 :skill-added!   add-skill!
                                 :skill-removed! remove-skill!
                                 :item-set!      set-item!
                                 :item-removed!  remove-item!}
                :entity/free-skill-points free-skill-points
                :entity/clickable {:type :clickable/player}
                :entity/click-distance-tiles click-distance-tiles}})

(defn- spawn-player-entity [ctx start-position player-props]
  (world/spawn-creature! ctx
                         (player-entity-props (utils/tile->middle start-position)
                                              player-props)))

(defn- spawn-enemies! [ctx tiled-map]
  (doseq [[position creature-id] (tiled/positions-with-property tiled-map :creatures :id)
          :let [props {:position position
                       :creature-id (keyword creature-id)
                       :components {:entity/fsm {:fsm :fsms/npc
                                                 :initial-state :npc-sleeping}
                                    :entity/faction :evil}}]]
    (world/spawn-creature! ctx (update props :position utils/tile->middle))))

; create 'world' record with data abstracted
; only problem probably spawn-entity! & inventory/ui callbacks?
; => return as 'txs' or 'events' to the main game!?

; => entity/tick! -> line of sight breaks world

(defn- create-game-state [{:keys [ctx/config] :as ctx} world-fn]
  (let [ctx (reduce (fn [ctx f]
                      (f ctx))
                    ctx
                    (:game-state-fns config))
        {:keys [tiled-map
                start-position]} (world-fn ctx)
        grid (grid-impl/create tiled-map)
        z-orders [:z-order/on-ground
                  :z-order/ground
                  :z-order/flying
                  :z-order/effect]
        ; so that at low fps the game doesn't jump faster between frames used @ movement to set a max speed so entities don't jump over other entities when checking collisions
        max-delta 0.04
        ; setting a min-size for colliding bodies so movement can set a max-speed for not
        ; skipping bodies at too fast movement
        ; TODO assert at properties load
        minimum-size 0.39 ; == spider smallest creature size.
        ; set max speed so small entities are not skipped by projectiles
        ; could set faster than max-speed if I just do multiple smaller movement steps in one frame
        max-speed (/ minimum-size max-delta)
        ctx (merge ctx
                   {:ctx/tiled-map tiled-map ; only @ cdq.render.draw-world-map -> pass graphics??
                    :ctx/elapsed-time 0 ; -> everywhere
                    :ctx/grid grid ; -> everywhere -> abstract ?
                    :ctx/raycaster (raycaster/create grid)
                    :ctx/content-grid (content-grid/create tiled-map (:content-grid-cell-size config))
                    :ctx/explored-tile-corners (atom (g2d/create-grid (tiled/tm-width  tiled-map)
                                                                      (tiled/tm-height tiled-map)
                                                                      (constantly false)))
                    :ctx/id-counter (atom 0)
                    :ctx/entity-ids (atom {})
                    :ctx/potential-field-cache (atom nil)
                    :ctx/factions-iterations (:potential-field-factions-iterations config)
                    :ctx/z-orders z-orders
                    :ctx/render-z-order (utils/define-order z-orders)
                    :ctx/minimum-size minimum-size
                    :ctx/max-delta max-delta
                    :ctx/max-speed max-speed})
        ctx (assoc ctx :ctx/player-eid (spawn-player-entity ctx
                                                            start-position
                                                            (:player-props config)))]
    (spawn-enemies! ctx tiled-map)
    ctx))

(defn do! [{:keys [ctx/config] :as ctx}]
  (create-game-state ctx (:world-fn config)))

(extend-type Context
  cdq.game/Game
  (reset-game-state! [ctx world-fn]
    (create-game-state ctx world-fn)))

(extend-type Context
  world/Context
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

(extend-type Context
  world/Grid
  (nearest-enemy-distance [{:keys [ctx/grid]} entity]
    (cell/nearest-entity-distance @(grid/cell grid (mapv int (entity/position entity)))
                                  (entity/enemy entity)))

  (nearest-enemy [{:keys [ctx/grid]} entity]
    (cell/nearest-entity @(grid/cell grid (mapv int (entity/position entity)))
                         (entity/enemy entity)))

  (potential-field-find-direction [{:keys [ctx/grid]} eid]
    (potential-fields.movement/find-direction grid eid)))

; does not take into account zoom - but zoom is only for debug ???
; vision range?
(defn- on-screen? [ctx position]
  (let [[x y] position
        x (float x)
        y (float y)
        [cx cy] (graphics/camera-position ctx)
        px (float cx)
        py (float cy)
        xdist (Math/abs (- x px))
        ydist (Math/abs (- y py))]
    (and
     (<= xdist (inc (/ (float (graphics/world-viewport-width  ctx))  2)))
     (<= ydist (inc (/ (float (graphics/world-viewport-height ctx)) 2))))))

; TODO at wrong point , this affects targeting logic of npcs
; move the debug flag to either render or mouseover or lets see
(def ^:private ^:dbg-flag los-checks? true)

(extend-type Context
  world/LineOfSight
  ; does not take into account size of entity ...
  ; => assert bodies <1 width then
  (line-of-sight? [{:keys [ctx/raycaster] :as ctx}
                   source
                   target]
    (and (or (not (:entity/player? source))
             (on-screen? ctx (entity/position target)))
         (not (and los-checks?
                   (raycaster/blocked? raycaster
                                       (entity/position source)
                                       (entity/position target)))))))

(extend-type Context
  world/SpawnEntity
  (spawn-entity! [ctx position body components]
    (cdq.ctx.spawn-entity/spawn-entity! ctx position body components)))

(extend-type Context
  world/Creatures
  (spawn-creature! [ctx opts]
    (cdq.ctx.spawn-creature/spawn-creature! ctx opts)))
