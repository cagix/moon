(ns cdq.world-fns.modules)

(defn- calculate-start-position [{:keys [start scale] :as w}]
  (assoc w :start-position (mapv * start scale)))

(defn create
  [{:keys [world/map-size
           world/max-area-level]
    :as world-fn-ctx}]
  (assert (<= max-area-level map-size))
  (reduce (fn [ctx f]
            (f ctx))
          (assoc world-fn-ctx :scale [32 20])
          (map requiring-resolve
               '[cdq.world-fns.modules.initial-grid/create
                 #_cdq.world-fns.modules.print/do!
                 cdq.world-fns.modules.assoc-transitions/step
                 #_cdq.world-fns.modules.print/do!
                 cdq.world-fns.modules.create-scaled-grid/step
                 cdq.world-fns.modules.load-schema-tiled-map/step
                 cdq.world-fns.modules.place-modules/do!
                 cdq.world-fns.modules.convert-to-tiled-map/step
                 cdq.world-fns.modules/calculate-start-position
                 cdq.world-fns.modules.finish/step])))
