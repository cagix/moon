(ns cdq.ctx.call-world-fn
  (:require [cdq.db :as db]
            [clojure.gdx.utils.disposable :as disposable]))

(defn do!
  [{:keys [ctx/db]
    :as ctx}
   world-fn]
  (let [{:keys [tiled-map
                start-position]} (let [[f params] world-fn]
                                   ((requiring-resolve f)
                                    (assoc params
                                           :creature-properties (db/all-raw db :properties/creatures)
                                           :ctx ctx)))]
    (assert tiled-map)
    (assert start-position)
    (when-let [tiled-map (:world/tiled-map (:ctx/world ctx))]
      (disposable/dispose! tiled-map))
    (assoc ctx :ctx/world {:world/tiled-map tiled-map
                           :world/start-position start-position})))
