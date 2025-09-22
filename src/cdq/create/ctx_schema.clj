(ns cdq.create.ctx-schema
  (:require [cdq.ctx :as ctx]
            [malli.core :as m]
            [malli.utils]))

(defn extend-validation [ctx schema]
  (let [schema (m/schema schema)]
    (extend-type (class ctx)
      ctx/Validation
      (validate [ctx]
        (malli.utils/validate-humanize schema ctx)
        ctx)))
  ctx)
