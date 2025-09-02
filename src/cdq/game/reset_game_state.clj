(ns cdq.game.reset-game-state
  (:require [cdq.ctx :as ctx]
            [cdq.ctx.db :as db]
            [cdq.ui.stage]
            [cdq.utils :as utils]
            [cdq.utils.tiled :as tiled]
            [cdq.world-impl]
            [cdq.ctx.world :as world]))

(defn- reset-stage!
  [{:keys [ctx/config
           ctx/stage]
    :as ctx}]
  (cdq.ui.stage/clear! stage)
  (doseq [actor (map #((requiring-resolve %) ctx)
                     (:create-ui-actors config))]
    (cdq.ui.stage/add! stage actor))
  ctx)

(defn- add-ctx-world
  [{:keys [ctx/config]
    :as ctx}
   world-fn]
  (assoc ctx :ctx/world (cdq.world-impl/create (merge (::world config)
                                                      (let [[f params] world-fn]
                                                        ((requiring-resolve f) ctx params))))))

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
       (world/spawn-creature! world)
       (ctx/handle-txs! ctx))
  (let [player-eid (get @(:world/entity-ids world) 1)]
    (assert (:entity/player? @player-eid))
    (assoc ctx :ctx/player-eid player-eid)))

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
         (world/spawn-creature! world)
         (ctx/handle-txs! ctx)))
  ctx)

; TODO dispose old tiled-map if already ctx/world present - or call 'dispose!'
(defn do! [ctx world-fn]
  (-> ctx
      reset-stage!
      (add-ctx-world world-fn)
      spawn-player!
      spawn-enemies!))

(def ^:private reset-game-state! do!)
