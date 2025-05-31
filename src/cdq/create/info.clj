(ns cdq.create.info
  (:require [cdq.g :as g]
            [cdq.g.info]))

(defn do! [ctx]
  (extend (class ctx)
    g/InfoText
    {:info-text (fn [ctx object]
                  (cdq.g.info/text ctx object))})
  ctx)
