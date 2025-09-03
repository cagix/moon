(ns cdq.game.reset-game-state
  (:require [cdq.ctx :as ctx]
            [cdq.db :as db]
            [cdq.ui.actor :as actor]
            [cdq.ui.stage :as stage]
            [cdq.utils :as utils]
            [cdq.utils.tiled :as tiled]))

(defn- add-ctx-world
  [{:keys [ctx/config]
    :as ctx}
   world-fn]
  (assoc ctx :ctx/world ((requiring-resolve (:world-impl config))
                         (merge (::world config)
                                (let [[f params] world-fn]
                                  ((requiring-resolve f) ctx params))))))

(defn- spawn-player!
  [{:keys [ctx/config
           ctx/db
           ctx/world]
    :as ctx}]
  (ctx/handle-txs! ctx
                   [[:tx/spawn-creature (let [{:keys [creature-id
                                                      components]} (:cdq.game/player-props config)]
                                          {:position (utils/tile->middle (:world/start-position world))
                                           :creature-property (db/build db creature-id)
                                           :components components})]])
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
    (ctx/handle-txs! ctx
                     [[:tx/spawn-creature {:position (utils/tile->middle position)
                                           :creature-property (db/build db (keyword creature-id))
                                           :components (:cdq.game/enemy-components config)}]]))
  ctx)

; TODO dispose old tiled-map if already ctx/world present - or call 'dispose!'
(defn do!
  [{:keys [ctx/config
           ctx/stage]
    :as ctx}
   world-fn]
  (stage/clear! stage)
  (doseq [actor-decl (map #((requiring-resolve %) ctx)
                          (:create-ui-actors config))]
    (stage/add! stage (actor/construct actor-decl)))
  (-> ctx
      (add-ctx-world world-fn)
      spawn-player!
      spawn-enemies!))
