(ns cdq.create.info
  (:require [cdq.info]
            [cdq.ctx.info]))

(defn do! [ctx]
  (extend (class ctx)
    cdq.info/Info
    {:text (fn [ctx object]
             (cdq.ctx.info/text ctx object))})
  ctx)
