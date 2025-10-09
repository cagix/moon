(ns cdq.game.create
  (:require [clojure.config :as config]))

(defn pipeline [ctx steps]
  (reduce (fn [ctx [f & params]]
            (apply (requiring-resolve f) ctx params))
          ctx
          steps))

(defn do! []
  (pipeline {}
            (config/edn-resource "create-pipeline.edn")))
