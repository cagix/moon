(ns cdq.render.validate
  (:require [cdq.malli :as m]))

(defn do! [{:keys [ctx/schema] :as ctx}]
  (m/validate-humanize schema ctx))
