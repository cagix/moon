(ns cdq.ctx.create-world
  (:require [gdl.txs :as txs]
            [cdq.db :as db]
            [cdq.graphics.textures :as textures]
            [cdq.world :as world]
            [cdq.world-fns.creature-tiles]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [com.badlogic.gdx.maps.tiled :as tiled]))

(defn- call-world-fn
  [world-fn creature-properties graphics]
  (let [[f params] (-> world-fn io/resource slurp edn/read-string)]
    ((requiring-resolve f)
     (assoc params
            :level/creature-properties (cdq.world-fns.creature-tiles/prepare creature-properties
                                                                             #(textures/texture-region graphics %))
            :textures (:graphics/textures graphics)))))

(def ^:private params
  {:content-grid-cell-size 16
   :world/factions-iterations {:good 15 :evil 5}
   :world/max-delta 0.04
   :world/minimum-size 0.39
   :world/z-orders [:z-order/on-ground
                    :z-order/ground
                    :z-order/flying
                    :z-order/effect]
   :world/enemy-components {:entity/fsm {:fsm :fsms/npc
                                         :initial-state :npc-sleeping}
                            :entity/faction :evil}
   :world/player-components {:creature-id :creatures/vampire
                             :components {:entity/fsm {:fsm :fsms/player
                                                       :initial-state :player-idle}
                                          :entity/faction :good
                                          :entity/player? true
                                          :entity/free-skill-points 3
                                          :entity/clickable {:type :clickable/player}
                                          :entity/click-distance-tiles 1.5}}
   :world/effect-body-props {:width 0.5
                             :height 0.5
                             :z-order :z-order/effect}})

(defn- spawn-player!
  [{:keys [ctx/db
           ctx/world]
    :as ctx}]
  (txs/handle! ctx
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
  (txs/handle!
   ctx
   (for [[position creature-id] (tiled/positions-with-property
                                 (:world/tiled-map world)
                                 "creatures"
                                 "id")]
     [:tx/spawn-creature {:position (mapv (partial + 0.5) position)
                          :creature-property (db/build db (keyword creature-id))
                          :components (:world/enemy-components world)}]))
  ctx)

(defn do! [{:keys [ctx/db
                   ctx/graphics
                   ctx/world]
            :as ctx}
           world-fn]
  (let [world-fn-result (call-world-fn world-fn
                                       (db/all-raw db :properties/creatures)
                                       graphics)]
    ; world/create returns txs ...
    ; applies it first itself?!
    (-> ctx
        (assoc :ctx/world (world/create params world-fn-result))
        spawn-player!
        spawn-enemies!)))
