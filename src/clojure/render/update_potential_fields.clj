(ns clojure.render.update-potential-fields
  (:require [clojure.potential-fields.update :as potential-fields.update]))

(defn- update-potential-fields!
  [{:keys [ctx/potential-field-cache
           ctx/factions-iterations
           ctx/grid
           ctx/active-entities]}]
  (doseq [[faction max-iterations] factions-iterations]
    (potential-fields.update/tick! potential-field-cache
                                   grid
                                   faction
                                   active-entities
                                   max-iterations)))

(defn do! [{:keys [ctx/paused?]
            :as ctx}]
  (if paused?
    ctx
    (do
     (update-potential-fields! ctx)
     ctx)))
