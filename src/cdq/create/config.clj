(ns cdq.create.config
  (:require [cdq.g :as g]
            [gdl.application]))

(def ^:private -k :ctx/config)

(defn add-config [ctx config]
  {:pre [(nil? (-k ctx))]}
  (assoc ctx -k config))

(extend-type gdl.application.Context
  g/Config
  (config [ctx key]
    (get (-k ctx) key)))
