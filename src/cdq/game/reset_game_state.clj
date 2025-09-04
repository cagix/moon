(ns cdq.game.reset-game-state
  (:require [cdq.ctx :as ctx]
            [cdq.db :as db]
            [cdq.ui.actor :as actor]
            [cdq.ui.stage :as stage]
            [cdq.utils :as utils]
            [cdq.utils.tiled :as tiled]))

(defn- assoc-player-eid [{:keys [ctx/world] :as ctx}]
  (let [eid (get @(:world/entity-ids world) 1)]
    (assert (:entity/player? @eid))
    (assoc ctx :ctx/player-eid eid)))

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
  ctx)

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
; TODO is this not a 'tx/' ???
; can I just [:tx/reset-game-state] somewhere ?
; tx.game/?
; then even at cdq.start ? just [:tx.app/] ?
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
      (assoc :ctx/world ((requiring-resolve (:world-impl config))
                         (merge (::world config)
                                (let [[f params] world-fn]
                                  ((requiring-resolve f) ctx params)))))
      spawn-player!
      assoc-player-eid
      spawn-enemies!))
