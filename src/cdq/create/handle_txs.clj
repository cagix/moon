(ns cdq.create.handle-txs
  (:require [cdq.ctx :as ctx]
            [cdq.ctx.effect-handler]))

(defn do! [ctx]
  (extend (class ctx)
    ctx/EffectHandler
    {:handle-txs! cdq.ctx.effect-handler/handle-txs!})
  ctx)
