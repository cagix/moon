(ns cdq.application.create.validation
  (:require [cdq.ctx :as ctx]
            [cdq.malli :as m]))

(def ^:private schema
  (m/schema
   [:map {:closed true}
    [:ctx/audio :some]
    [:ctx/db :some]
    [:ctx/graphics :some]
    [:ctx/input :some]
    [:ctx/stage :some]
    [:ctx/vis-ui :some]
    [:ctx/world :some]]))

(defn do! [ctx]
  (extend-type (class ctx)
    ctx/Validation
    (validate [ctx]
      (m/validate-humanize schema ctx)
      ctx))
  ctx)
