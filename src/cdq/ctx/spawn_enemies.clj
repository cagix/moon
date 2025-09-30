(ns cdq.ctx.spawn-enemies
  (:require [cdq.ctx :as ctx]
            [cdq.db :as db]
            [com.badlogic.gdx.maps.tiled :as tiled]))

(defn do!
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
