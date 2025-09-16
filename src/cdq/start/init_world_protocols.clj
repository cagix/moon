(ns cdq.start.init-world-protocols
  (:require [cdq.create.world-config]
            [cdq.ctx.world]))

(defn do! [ctx]
  (extend-type cdq.create.world_config.World
    cdq.ctx.world/Resettable
    (reset-state [world world-fn-result]
      (cdq.world.resettable/reset-state world world-fn-result)))
  ctx)
