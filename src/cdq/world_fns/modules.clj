(ns cdq.world-fns.modules
  (:require cdq.world-fns.modules.add-scale
            cdq.world-fns.modules.add-scale
            cdq.world-fns.modules.assert-max-area-level
            cdq.world-fns.modules.create-initial-grid
            cdq.world-fns.modules.print-grid
            cdq.world-fns.modules.assoc-transitions
            cdq.world-fns.modules.print-grid
            cdq.world-fns.modules.create-scaled-grid
            cdq.world-fns.modules.load-schema-tiled-map
            cdq.world-fns.modules.place-modules
            cdq.world-fns.modules.convert-to-tiled-map
            cdq.world-fns.modules.calculate-start-position
            cdq.world-fns.modules.generate-modules))

(defn create
  [world-fn-ctx]
  (-> world-fn-ctx
      cdq.world-fns.modules.add-scale/do! ; :scale
      cdq.world-fns.modules.assert-max-area-level/do!
      cdq.world-fns.modules.create-initial-grid/do! ; :grid
      #_cdq.world-fns.modules.print-grid/do!
      cdq.world-fns.modules.assoc-transitions/do! ; :grid
      #_cdq.world-fns.modules.print-grid/do!
      cdq.world-fns.modules.create-scaled-grid/do! ; :scaled-grid
      cdq.world-fns.modules.load-schema-tiled-map/do! ; :schema-tiled-map
      cdq.world-fns.modules.place-modules/do! ; :scaled-grid
      cdq.world-fns.modules.convert-to-tiled-map/do! ; :tiled-map
      cdq.world-fns.modules.calculate-start-position/do! ; :start-position
      cdq.world-fns.modules.generate-modules/do!))
