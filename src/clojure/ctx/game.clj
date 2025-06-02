(ns clojure.ctx.game
  (:require [clojure.ctx :as ctx]
            [clojure.content-grid :as content-grid]
            [clojure.grid2d :as g2d]
            [clojure.grid-impl :as grid-impl]
            [clojure.raycaster :as raycaster]
            [clojure.state :as state]
            [clojure.ui.action-bar :as action-bar]
            [clojure.ui.windows.inventory :as inventory-window]
            [clojure.tiled :as tiled]
            [clojure.utils :as utils]))

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
  (ctx/spawn-creature! ctx
                       (player-entity-props (utils/tile->middle start-position)
                                            player-props)))

(defn- spawn-enemies! [ctx tiled-map]
  (doseq [[position creature-id] (tiled/positions-with-property tiled-map :creatures :id)
          :let [props {:position position
                       :creature-id (keyword creature-id)
                       :components {:entity/fsm {:fsm :fsms/npc
                                                 :initial-state :npc-sleeping}
                                    :entity/faction :evil}}]]
    (ctx/spawn-creature! ctx (update props :position utils/tile->middle))))

(defn reset-game-state! [{:keys [ctx/config] :as ctx} world-fn]
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
                   {:ctx/tiled-map tiled-map ; only @ clojure.render.draw-world-map -> pass graphics??
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
