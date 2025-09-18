(ns cdq.malli
  (:require [malli.core :as m]
            [malli.error :as me]
            [malli.generator :as mg]))

(defn generate [form opts]
  (mg/generate form opts))

(defn validate-humanize [schema value]
  (when-not (m/validate schema value)
    (throw (ex-info (str (me/humanize (m/explain schema value)))
                    {:value value
                     :schema (m/form schema)}))))
