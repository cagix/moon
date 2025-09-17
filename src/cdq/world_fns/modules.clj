(ns cdq.world-fns.modules)

(defn create
  [world-fn-ctx]
  (-> world-fn-ctx
      ((requiring-resolve 'cdq.world-fns.modules.add-scale/do!)) ; :scale
      ((requiring-resolve 'cdq.world-fns.modules.assert-max-area-level/do!))
      ((requiring-resolve 'cdq.world-fns.modules.prepare-creature-properties/do!)) ; :creature-properties
      ((requiring-resolve 'cdq.world-fns.modules.create-initial-grid/do!)) ; :grid
      ((requiring-resolve 'cdq.world-fns.modules.print-grid/do!))
      ((requiring-resolve 'cdq.world-fns.modules.assoc-transitions/do!)) ; :grid
      ((requiring-resolve 'cdq.world-fns.modules.print-grid/do!))
      ((requiring-resolve 'cdq.world-fns.modules.create-scaled-grid/do!)) ; :scaled-grid
      ((requiring-resolve 'cdq.world-fns.modules.load-schema-tiled-map/do!)) ; :schema-tiled-map
      ((requiring-resolve 'cdq.world-fns.modules.place-modules/do!)) ; :scaled-grid
      ((requiring-resolve 'cdq.world-fns.modules.convert-to-tiled-map/do!)) ; :tiled-map
      ((requiring-resolve 'cdq.world-fns.modules.calculate-start-position/do!)) ; :start-position
      ((requiring-resolve 'cdq.world-fns.modules.generate-modules/do!))))
