(ns cdq.reset-game-state
  (:require [cdq.grid-impl :as grid-impl]
            [cdq.raycaster :as raycaster]
            [cdq.ctx :as ctx]
            [cdq.db :as db]
            [cdq.grid2d :as g2d]
            [cdq.ui.actor :as actor]
            [cdq.ui.stage :as stage]
            [cdq.utils :as utils]
            [cdq.utils.tiled :as tiled]
            [cdq.content-grid :as content-grid]))

(defn- world-ctx
  [{:keys [tiled-map] :as config}]
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
    {:ctx/grid grid
     :ctx/raycaster (raycaster/create grid)
     :ctx/elapsed-time 0
     :ctx/max-delta max-delta
     :ctx/max-speed max-speed
     :ctx/minimum-size minimum-size
     :ctx/z-orders z-orders}))

(defn- create-explored-tile-corners [tiled-map]
  (atom (g2d/create-grid (:tiled-map/width  tiled-map)
                         (:tiled-map/height tiled-map)
                         (constantly false))))

(defn- assoc-player-eid
  [{:keys [ctx/entity-ids]
    :as ctx}]
  (let [eid (get @entity-ids 1)]
    (assert (:entity/player? @eid))
    (assoc ctx :ctx/player-eid eid)))

(defn- spawn-player!
  [{:keys [ctx/config
           ctx/db]
    :as ctx}
   start-position]
  (ctx/handle-txs! ctx
                   [[:tx/spawn-creature (let [{:keys [creature-id
                                                      components]} (:cdq.game/player-props config)]
                                          {:position (utils/tile->middle start-position)
                                           :creature-property (db/build db creature-id)
                                           :components components})]])
  ctx)

(defn- spawn-enemies!
  [{:keys [ctx/config
           ctx/db
           ctx/tiled-map]
    :as ctx}]
  (doseq [[position creature-id] (tiled/positions-with-property tiled-map "creatures" "id")]
    (ctx/handle-txs! ctx
                     [[:tx/spawn-creature {:position (utils/tile->middle position)
                                           :creature-property (db/build db (keyword creature-id))
                                           :components (:cdq.game/enemy-components config)}]]))
  ctx)

; TODO dispose old ctx/tiled-map if already present
; TODO is this not a 'tx/' ???
; can I just [:tx/reset-game-state] somewhere ?
; tx.game/?
; then even at cdq.start ? just [:tx.app/] ?
; this is just a bunch of functions in the context of 'cdq.reset-game-state' ...
(defn do!
  [{:keys [ctx/config
           ctx/stage]
    :as ctx}
   world-fn]
  (stage/clear! stage)
  (doseq [actor-decl (map #(let [[f params] %]
                             ((requiring-resolve f) ctx params))
                          (:create-ui-actors config))]
    (stage/add! stage (actor/construct actor-decl)))
  (let [world-config (merge (::world config)
                            (let [[f params] world-fn]
                              ((requiring-resolve f) ctx params)))
        world-ctx* (world-ctx world-config)]
    (-> ctx
        (merge world-ctx*)
        (assoc :ctx/tiled-map (:tiled-map world-config))
        (assoc :ctx/explored-tile-corners (create-explored-tile-corners (:tiled-map world-config)))
        (assoc :ctx/content-grid (content-grid/create (:tiled-map/width  (:tiled-map world-config))
                                                      (:tiled-map/height (:tiled-map world-config))
                                                      (:content-grid-cell-size world-config)))
        (assoc :ctx/potential-field-cache (atom nil))
        (assoc :ctx/factions-iterations (:potential-field-factions-iterations world-config))
        (assoc :ctx/id-counter (atom 0))
        (assoc :ctx/entity-ids (atom {}))
        (assoc :ctx/render-z-order (utils/define-order (:ctx/z-orders world-ctx*)))
        (spawn-player! (:start-position world-config))
        assoc-player-eid
        spawn-enemies!)))
