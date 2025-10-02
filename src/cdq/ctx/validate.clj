(ns cdq.ctx.validate
  (:require [cdq.malli :as m]))

(def ^:private schema
  (m/schema
   [:map {:closed true}
    [:ctx/audio :some]
    [:ctx/db :some]
    [:ctx/graphics :some]
    [:ctx/input :some]
    [:ctx/stage :some]
    [:ctx/actor-fns :some]
    [:ctx/vis-ui :some]
    [:ctx/world :some]]))

(defn do! [ctx]
  (m/validate-humanize schema ctx)
  ctx)
