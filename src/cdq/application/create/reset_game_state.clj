(ns cdq.application.create.reset-game-state
  (:require [cdq.ctx :as ctx]
            [cdq.db :as db]
            [cdq.graphics :as graphics]
            [cdq.stage :as stage]
            [cdq.world :as world]
            [cdq.world-fns.creature-tiles]
            [gdl.disposable :as disposable]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [gdl.tiled :as tiled]))

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

(defn- reset-stage-actors!
  [{:keys [ctx/db
           ctx/graphics
           ctx/stage]
    :as ctx}]
  (stage/rebuild-actors! stage db graphics)
  ctx)

(defn- call-world-fn
  [world-fn creature-properties graphics]
  (let [[f params] (-> world-fn io/resource slurp edn/read-string)]
    ((requiring-resolve f)
     (assoc params
            :level/creature-properties (cdq.world-fns.creature-tiles/prepare creature-properties
                                                                             #(graphics/texture-region graphics %))
            :textures (:graphics/textures graphics)))))

(defn- reset-world-state
  [{:keys [ctx/db
           ctx/graphics]
    :as ctx}
   world-fn]
  (let [world-fn-result (call-world-fn world-fn
                                       (db/all-raw db :properties/creatures)
                                       graphics)]
    (update ctx :ctx/world world/reset-state world-fn-result)))

(def starting-world-fn "world_fns/vampire.edn")

(defn do! [ctx]
  (extend-type (class ctx)
    ctx/ResetGameState
    (reset-game-state! [{:keys [ctx/world]
                         :as ctx}
                        world-fn]
      (disposable/dispose! world)
      (-> ctx
          reset-stage-actors!
          (reset-world-state world-fn)
          spawn-player!
          spawn-enemies!)))
  (ctx/reset-game-state! ctx starting-world-fn))
