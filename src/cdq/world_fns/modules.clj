(ns cdq.world-fns.modules)

(defn create
  [world-fn-ctx]
  (-> world-fn-ctx
      ((requiring-resolve 'cdq.world-fns.modules.add-scale/do!))
      ((requiring-resolve 'cdq.world-fns.modules.assert-max-area-level/do!))
      ((requiring-resolve 'cdq.world-fns.modules.prepare-creature-properties/do!))
      ((requiring-resolve 'cdq.world-fns.modules.create-initial-grid/do!))
      ((requiring-resolve 'cdq.world-fns.modules.print-grid/do!))
      ((requiring-resolve 'cdq.world-fns.modules.assoc-transitions/do!))
      ((requiring-resolve 'cdq.world-fns.modules.print-grid/do!))
      ((requiring-resolve 'cdq.world-fns.modules.create-scaled-grid/do!))
      ((requiring-resolve 'cdq.world-fns.modules.place-modules/do!))
      ((requiring-resolve 'cdq.world-fns.modules.calculate-start-position/do!))
      ((requiring-resolve 'cdq.world-fns.modules.generate-modules/do!))))
