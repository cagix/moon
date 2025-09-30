(ns cdq.c
  (:require [cdq.ctx.reset-stage-actors :as reset-stage-actors]
            cdq.scene2d.build.editor-overview-window
            cdq.scene2d.build.editor-window
            [clj-commons.pretty.repl :as pretty-repl]
            [cdq.ctx :as ctx]
            [clojure.edn :as edn]
            [clojure.string :as str]
            [clojure.java.io :as io]
            [cdq.audio :as audio]
            [cdq.ctx :as ctx]
            [cdq.db :as db]
            [cdq.entity.stats :as stats]
            [cdq.entity.state :as state]
            cdq.entity.state.player-item-on-cursor
            [cdq.graphics :as graphics]
            [cdq.info :as info]
            [cdq.input :as input]
            [cdq.stage]
            [cdq.world :as world]
            [cdq.world-fns.creature-tiles]
            [cdq.world.content-grid :as content-grid]
            [com.badlogic.gdx.maps.tiled :as tiled]
            [com.badlogic.gdx.scenes.scene2d :as scene2d]
            [com.badlogic.gdx.utils.disposable :as disposable]
            [gdl.utils :as utils]))

(defn- spawn-enemies!
  [{:keys [ctx/db
           ctx/world]
    :as ctx}]
  (ctx/handle-txs!
   ctx
   (for [[position creature-id] (tiled/positions-with-property
                                 (:world/tiled-map world)
                                 "creatures"
                                 "id")]
     [:tx/spawn-creature {:position (mapv (partial + 0.5) position)
                          :creature-property (db/build db (keyword creature-id))
                          :components (:world/enemy-components world)}]))
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

(defn create! [ctx]
  (extend-type (class ctx)
    ctx/ResetGameState
    (reset-game-state! [{:keys [ctx/world]
                         :as ctx}
                        world-fn]
      (disposable/dispose! world)
      (-> ctx
          reset-stage-actors/do!
          (reset-world-state world-fn)
          spawn-player!
          spawn-enemies!)) )
  (-> ctx
      (ctx/reset-game-state! "world_fns/vampire.edn")))
