(ns cdq.create.info
  (:require [cdq.info]
            [cdq.g.info]))

(defn do! [ctx]
  (extend (class ctx)
    cdq.info/Info
    {:text (fn [ctx object]
             (cdq.g.info/text ctx object))})
  ctx)
