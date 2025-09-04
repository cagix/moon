(ns cdq.game.reset-game-state
  (:require [cdq.ctx :as ctx]
            [cdq.db :as db]
            [cdq.grid2d :as g2d]
            [cdq.ui.actor :as actor]
            [cdq.ui.stage :as stage]
            [cdq.utils :as utils]
            [cdq.utils.tiled :as tiled]
            [cdq.world.content-grid :as content-grid]))

(defn- create-explored-tile-corners [tiled-map]
  (atom (g2d/create-grid (:tiled-map/width  tiled-map)
                         (:tiled-map/height tiled-map)
                         (constantly false))))

(defn- assoc-player-eid [{:keys [ctx/world] :as ctx}]
  (let [eid (get @(:world/entity-ids world) 1)]
    (assert (:entity/player? @eid))
    (assoc ctx :ctx/player-eid eid)))

(defn- spawn-player!
  [{:keys [ctx/config
           ctx/db
           ctx/world]
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

; TODO dispose old tiled-map if already ctx/world present - or call 'dispose!'
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
  (doseq [actor-decl (map #((requiring-resolve %) ctx)
                          (:create-ui-actors config))]
    (stage/add! stage (actor/construct actor-decl)))
  (let [world-config (merge (::world config)
                            (let [[f params] world-fn]
                              ((requiring-resolve f) ctx params)))]
    (-> ctx
        (assoc :ctx/tiled-map (:tiled-map world-config))
        (assoc :ctx/explored-tile-corners (create-explored-tile-corners (:tiled-map world-config)))
        (assoc :ctx/content-grid (content-grid/create (:tiled-map/width  (:tiled-map world-config))
                                                      (:tiled-map/height (:tiled-map world-config))
                                                      (:content-grid-cell-size world-config)))
        (assoc :ctx/world ((requiring-resolve (:world-impl config)) world-config))
        (assoc :ctx/potential-field-cache (atom nil))
        (assoc :ctx/factions-iterations (:potential-field-factions-iterations world-config))
        (spawn-player! (:start-position world-config))
        assoc-player-eid
        spawn-enemies!)))
