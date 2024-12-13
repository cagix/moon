(ns anvil.operation
  (:refer-clojure :exclude [remove])
  (:require [anvil.component :as component]
            [anvil.info :as info]
            [clojure.math :as math]
            [clojure.string :as str]
            [gdl.utils :refer [defmethods]]))

(defmethods :op/inc
  (component/value-text [[_ value]]
    (str value))

  (component/apply [[_ value] base-value]
    (+ base-value value))

  (component/order [_]
    0))

(defmethods :op/mult
  (component/value-text [[_ value]]
    (str value "%"))

  (component/apply [[_ value] base-value]
    (* base-value (inc (/ value 100))))

  (component/order [_]
    1))

(defn apply [op value]
  (reduce (fn [value op]
            (component/apply op value))
          value
          (sort-by component/order op)))

(defn add    [op other-op] (merge-with + op other-op))
(defn remove [op other-op] (merge-with - op other-op))

(defn- +? [n]
  (case (math/signum n)
    0.0 ""
    1.0 "+"
    -1.0 ""))

(defn info [op k]
  (str/join "\n"
            (keep
             (fn [{v 1 :as component}]
               (when-not (zero? v)
                 (str (+? v) (component/value-text component) " " (info/k->pretty-name k))))
             (sort-by component/order op))))
