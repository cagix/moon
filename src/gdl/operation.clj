(ns gdl.operation
  "Namespace for operations in the game engine. Provides utilities for applying, combining,
   and retrieving information about operations, such as incremental and multiplicative effects."
  (:refer-clojure :exclude [apply remove])
  (:require [clojure.component :refer [defsystem defcomponent]]
            [clojure.math :as math]
            [clojure.string :as str]
            [clojure.utils :refer [k->pretty-name]]))

(defsystem ^:private -apply)
(defsystem ^:private -order)
(defsystem ^:private -value-text)

(defcomponent :op/inc
  (-value-text [[_ value]]
    (str value))

  (-apply [[_ value] base-value]
    (+ base-value value))

  (-order [_]
    0))

(defcomponent :op/mult
  (-value-text [[_ value]]
    (str value "%"))

  (-apply [[_ value] base-value]
    (* base-value (inc (/ value 100))))

  (-order [_]
    1))

(defn apply
  "Applies a sequence of operations (`op`) to an initial `value`.
  Operations are applied in a specific order determined by `-order`.

  Args:
  - `op`: A sequence of operations.
  - `value`: The initial value to which the operations will be applied.

  Returns:
  The resulting value after all operations are applied."
  [op value]
  (reduce (fn [value op]
            (-apply op value))
          value
          (sort-by -order op)))

(defn add
  "Combines two operations (`op` and `other-op`) by summing their effects.

  Args:
  - `op`: The first operation map.
  - `other-op`: The second operation map.

  Returns:
  A new operation map with combined effects."
  [op other-op]
  (merge-with + op other-op))

(defn remove
  "Subtracts the effects of one operation (`other-op`) from another (`op`).

  Args:
  - `op`: The initial operation map.
  - `other-op`: The operation map to subtract.

  Returns:
  A new operation map with subtracted effects."
  [op other-op]
  (merge-with - op other-op))

(defn- +? [n]
  (case (math/signum n)
    0.0 ""
    1.0 "+"
    -1.0 ""))

(defn info
  "Generates a human-readable summary of an operation (`op`) for a specific key (`k`).

  Args:
  - `op`: A sequence of operation components.
  - `k`: The key associated with the operation for pretty printing.

  Returns:
  A string with details of the operation components, their values, and a description."
  [op k]
  (str/join "\n"
            (keep
             (fn [{v 1 :as component}]
               (when-not (zero? v)
                 (str (+? v) (-value-text component) " " (k->pretty-name k))))
             (sort-by -order op))))
