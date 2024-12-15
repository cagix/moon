(ns gdl.val-max
  (:require [malli.core :as m]))

(def schema-form
  [:and
   [:vector {:min 2 :max 2} [:int {:min 0}]]
   [:fn {:error/fn (fn [{[^int v ^int mx] :value} _]
                     (when (< mx v)
                       (format "Expected max (%d) to be smaller than val (%d)" v mx)))}
    (fn [[^int a ^int b]] (<= a b))]])

(def schema
  (m/schema schema-form))

(defn ratio
  "If mx and v is 0, returns 0, otherwise (/ v mx)"
  [[^int v ^int mx]]
  {:pre [(m/validate schema [v mx])]}
  (if (and (zero? v) (zero? mx))
    0
    (/ v mx)))
