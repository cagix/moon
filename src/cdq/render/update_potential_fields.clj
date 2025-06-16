(ns cdq.render.update-potential-fields
  (:require [cdq.w :as w]))

(defn do!
  [{:keys [ctx/world]
    :as ctx}]
  (w/tick-potential-fields! world)
  ctx)
