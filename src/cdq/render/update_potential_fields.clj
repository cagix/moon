(ns cdq.render.update-potential-fields
  (:require [cdq.world :as world]))

(defn do! [{:keys [ctx/paused?]
            :as ctx}]
  (if paused?
    ctx
    (do
     (world/update-potential-fields! ctx)
     ctx)))
