(ns cdq.application.create.record
  (:require [cdq.ctx :as ctx]
            [cdq.malli :as m]
            [qrecord.core :as q]))

(q/defrecord Context [
                      ctx/graphics
                      ])

(defn do! [ctx schema-form]
  (let [schema (m/schema schema-form)]
    (extend-type Context
      ctx/Validation
      (validate [ctx]
        (m/validate-humanize schema ctx)
        ctx)))
  (merge (map->Context {})
         ctx))
