(ns cdq.application.create.reset-world
  (:require [cdq.db :as db]
            [cdq.world :as world]
            [cdq.world-fns.creature-tiles]))

(defn- call-world-fn
  [[f params] creature-properties graphics]
  (f
   (assoc params
          :level/creature-properties (cdq.world-fns.creature-tiles/prepare creature-properties graphics)
          :textures (:graphics/textures graphics))))

(defn do!
  [{:keys [ctx/db
           ctx/graphics]
    :as ctx}
   world-fn]
  (let [world-fn-result (call-world-fn world-fn
                                       (db/all-raw db :properties/creatures)
                                       graphics)]
    (update ctx :ctx/world world/reset-state world-fn-result)))
