(ns cdq.ctx.reset-world-state
  (:require [cdq.ctx.spawn-player :as spawn-player]
            [cdq.ctx.spawn-enemies :as spawn-enemies]
            [cdq.db :as db]
            [cdq.graphics :as graphics]
            [cdq.world :as world]
            [cdq.world-fns.creature-tiles]
            [clojure.edn :as edn]
            [clojure.java.io :as io]))

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

(defn do! [{:keys [ctx/world]
            :as ctx}
           world-fn]
  (-> ctx
      (reset-world-state world-fn)
      spawn-player/do!
      spawn-enemies/do!))
