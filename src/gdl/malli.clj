(ns gdl.malli
  (:require [malli.core :as m]
            [malli.error :as me]))

; 1. move all malli stuff here
; 2. replace by clojure.spec

(defn- invalid-ex-info [malli-schema value]
  (ex-info (str (me/humanize (m/explain malli-schema value)))
           {:value value
            :schema (m/form malli-schema)}))

(defn validate! [malli-schema-form data]
  (let [malli-schema (m/schema malli-schema-form)]
    (when-not (m/validate malli-schema data)
      (throw (invalid-ex-info malli-schema data)))))
