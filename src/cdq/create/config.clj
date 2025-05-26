(ns cdq.create.config
  (:require [cdq.g :as g]))

(def ^:private -k :ctx/config)

(defn add-config [ctx config]
  {:pre [(nil? (-k ctx))]}
  (extend (class ctx)
    g/Config
    {:config (fn [ctx key]
               (get (-k ctx) key))})
  (assoc ctx -k config))
