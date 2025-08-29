(ns cdq.ctx.game
  (:require [cdq.ctx :as ctx]
            [cdq.content-grid :as content-grid]
            [cdq.db :as db]
            [cdq.grid2d :as g2d]
            [cdq.grid-impl :as grid-impl]
            [cdq.raycaster :as raycaster]
            [cdq.utils :as utils]
            [cdq.utils.tiled :as tiled]
            [cdq.world :as w]
            cdq.ui.dev-menu
            cdq.ui.action-bar
            cdq.ui.hp-mana-bar
            cdq.ui.windows.entity-info
            cdq.ui.windows.inventory
            cdq.entity.state.player-idle
            cdq.entity.state.player-item-on-cursor
            cdq.ui.player-state-draw
            cdq.ui.message
            [cdq.ui.stage :as stage]
            [qrecord.core :as q]))

(defn- create-ui-actors [ctx]
  [(cdq.ui.dev-menu/create ctx ;graphics db
                           {:world-fns '[[cdq.level.from-tmx/create {:tmx-file "maps/vampire.tmx"
                                                                     :start-position [32 71]}]
                                         [cdq.level.uf-caves/create {:tile-size 48
                                                                     :texture "maps/uf_terrain.png"
                                                                     :spawn-rate 0.02
                                                                     :scaling 3
                                                                     :cave-size 200
                                                                     :cave-style :wide}]
                                         [cdq.level.modules/create {:world/map-size 5,
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
     :actors [(cdq.ui.windows.entity-info/create ctx {:y 0}) ; graphics only
              (cdq.ui.windows.inventory/create ctx ; graphics only
               {:title "Inventory"
                :id :inventory-window
                :visible? false
                :state->clicked-inventory-cell
                {:player-idle           cdq.entity.state.player-idle/clicked-inventory-cell
                 :player-item-on-cursor cdq.entity.state.player-item-on-cursor/clicked-cell}})]}
    (cdq.ui.player-state-draw/create
     {:state->draw-gui-view
      {:player-item-on-cursor
       cdq.entity.state.player-item-on-cursor/draw-gui-view}})
    (cdq.ui.message/create {:duration-seconds 0.5
                            :name "player-message"})])

(defn- reset-stage!
  [{:keys [ctx/stage]
    :as ctx}]
  (stage/clear! stage)
  (doseq [actor (create-ui-actors ctx)]
    (stage/add! stage actor))
  ctx)

(defn- create-explored-tile-corners [tiled-map]
  (atom (g2d/create-grid (:tiled-map/width  tiled-map)
                         (:tiled-map/height tiled-map)
                         (constantly false))))

(q/defrecord World [world/tiled-map
                    world/start-position
                    world/grid
                    world/explored-tile-corners
                    world/content-grid
                    world/raycaster
                    world/potential-field-cache
                    world/factions-iterations
                    world/id-counter
                    world/entity-ids
                    world/elapsed-time
                    world/max-delta
                    world/max-speed
                    world/minimum-size
                    world/z-orders
                    world/render-z-order

                    ; added later
                    world/delta-time
                    world/paused?
                    world/active-entities
                    world/mouseover-eid
                    world/player-eid])

(defn- create-world
  [{:keys [tiled-map
           start-position]
    :as config}]
  (let [grid (grid-impl/create tiled-map)
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
        max-speed (/ minimum-size max-delta)]
    (merge (map->World {})
           {:world/tiled-map tiled-map
            :world/start-position start-position
            :world/grid grid
            :world/explored-tile-corners (create-explored-tile-corners tiled-map)
            :world/content-grid (content-grid/create (:tiled-map/width  tiled-map)
                                                     (:tiled-map/height tiled-map)
                                                     (:content-grid-cell-size config))
            :world/raycaster (raycaster/create grid)
            :world/potential-field-cache (atom nil)
            :world/factions-iterations (:potential-field-factions-iterations config)
            :world/id-counter (atom 0)
            :world/entity-ids (atom {})
            :world/elapsed-time 0
            :world/max-delta max-delta
            :world/max-speed max-speed
            :world/minimum-size minimum-size
            :world/z-orders z-orders
            :world/render-z-order (utils/define-order z-orders)})))

(defn- add-ctx-world
  [{:keys [ctx/config]
    :as ctx}
   world-fn]
  (assoc ctx :ctx/world (create-world (merge (:cdq.ctx.game/world config)
                                             (let [[f params] world-fn]
                                               (f ctx params))))))

(defn- spawn-player!
  [{:keys [ctx/config
           ctx/db
           ctx/world]
    :as ctx}]
  (->> (let [{:keys [creature-id
                     components]} (:cdq.ctx.game/player-props config)]
         {:position (utils/tile->middle (:world/start-position world))
          :creature-property (db/build db creature-id)
          :components components})
       (w/spawn-creature! world)
       (ctx/handle-txs! ctx))
  (let [player-eid (get @(:world/entity-ids world) 1)]
    (assert (:entity/player? @player-eid))
    (assoc-in ctx [:ctx/world :world/player-eid] player-eid)))

(defn- spawn-enemies!
  [{:keys [ctx/config
           ctx/db
           ctx/world]
    :as ctx}]
  (doseq [[position creature-id] (tiled/positions-with-property (:world/tiled-map world)
                                                                "creatures"
                                                                "id")]
    (->> {:position (utils/tile->middle position)
          :creature-property (db/build db (keyword creature-id))
          :components (:cdq.ctx.game/enemy-components config)}
         (w/spawn-creature! world)
         (ctx/handle-txs! ctx)))
  ctx)

(defn reset-game-state! [ctx world-fn]
  (-> ctx
      reset-stage!
      (add-ctx-world world-fn)
      spawn-player!
      spawn-enemies!))
