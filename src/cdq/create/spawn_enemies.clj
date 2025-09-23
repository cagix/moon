(ns cdq.create.spawn-enemies
  (:require [cdq.ctx :as ctx]
            [cdq.db :as db]
            [clojure.tiled :as tiled]))

(defn do!
  [{:keys [ctx/db
           ctx/world]
    :as ctx}]
  (doseq [[position creature-id] (tiled/positions-with-property (:world/tiled-map world) "creatures" "id")]
    (ctx/handle-txs! ctx
                     [[:tx/spawn-creature {:position (mapv (partial + 0.5) position)
                                           :creature-property (db/build db (keyword creature-id))
                                           :components (:world/enemy-components world)}]]))
  ctx)
