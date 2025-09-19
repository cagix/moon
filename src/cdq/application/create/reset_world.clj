(ns cdq.application.create.reset-world
  (:require [cdq.ctx :as ctx]
            [cdq.db :as db]
            [cdq.world :as world]
            [gdl.tiled :as tiled]))

(defn- call-world-fn
  [[f params] creature-properties graphics]
  (f
   (assoc params
          :creature-properties creature-properties
          :graphics graphics)))

(defn- reset-world
  [{:keys [ctx/db
           ctx/graphics]
    :as ctx}
   world-fn]
  (let [world-fn-result (call-world-fn world-fn
                                       (db/all-raw db :properties/creatures)
                                       graphics)]
    (update ctx :ctx/world world/reset-state world-fn-result)))

(defn- spawn-player!
  [{:keys [ctx/db
           ctx/world]
    :as ctx}]
  (ctx/handle-txs! ctx
                   [[:tx/spawn-creature (let [{:keys [creature-id
                                                      components]} (:world/player-components world)]
                                          {:position (mapv (partial + 0.5) (:world/start-position world))
                                           :creature-property (db/build db creature-id)
                                           :components components})]])
  (let [eid (get @(:world/entity-ids world) 1)]
    (assert (:entity/player? @eid))
    (assoc-in ctx [:ctx/world :world/player-eid] eid)))

(defn- spawn-enemies!
  [{:keys [ctx/db
           ctx/world]
    :as ctx}]
  (doseq [[position creature-id] (tiled/positions-with-property (:world/tiled-map world) "creatures" "id")]
    (ctx/handle-txs! ctx
                     [[:tx/spawn-creature {:position (mapv (partial + 0.5) position)
                                           :creature-property (db/build db (keyword creature-id))
                                           :components (:world/enemy-components world)}]]))
  ctx)

(defn do!
  [ctx world-fn]
  (-> ctx
      (reset-world world-fn)
      spawn-player!
      spawn-enemies!))
