(ns moon.val-max
  (:require [moon.schema :as schema]
            [malli.core :as m]
            [moon.entity.modifiers :as mods]))

(def schema
  (m/schema [:and
             [:vector {:min 2 :max 2} [:int {:min 0}]]
             [:fn {:error/fn (fn [{[^int v ^int mx] :value} _]
                               (when (< mx v)
                                 (format "Expected max (%d) to be smaller than val (%d)" v mx)))}
              (fn [[^int a ^int b]] (<= a b))]]))

(defmethod schema/form :s/val-max [_]
  (m/form schema))

(defn ratio
  "If mx and v is 0, returns 0, otherwise (/ v mx)"
  [[^int v ^int mx]]
  {:pre [(m/validate schema [v mx])]}
  (if (and (zero? v) (zero? mx))
    0
    (/ v mx)))

(defn- ->pos-int [val-max]
  (mapv #(-> % int (max 0)) val-max))

(defn apply-max-modifier [val-max entity modifier-k]
  {:pre  [(m/validate schema val-max)]
   :post [(m/validate schema val-max)]}
  (let [val-max (update val-max 1 mods/value entity modifier-k)
        [v mx] (->pos-int val-max)]
    [(min v mx) mx]))

(defn apply-min-modifier [val-max entity modifier-k]
  {:pre  [(m/validate schema val-max)]
   :post [(m/validate schema val-max)]}
  (let [val-max (update val-max 0 mods/value entity modifier-k)
        [v mx] (->pos-int val-max)]
    [v (max v mx)]))
