(ns cdq.application.reset-game-state)

(defn reset-game-state!
  [ctx world-fn]
  (-> ctx
      ((requiring-resolve 'cdq.ctx.call-world-fn/do!) world-fn)
      ((requiring-resolve 'cdq.ctx.build-world/do!))
      ((requiring-resolve 'cdq.ctx.spawn-player/do!))
      ((requiring-resolve 'cdq.ctx.spawn-enemies/do!))))
