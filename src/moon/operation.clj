(ns moon.operation
  (:refer-clojure :exclude [apply])
  (:require [clojure.string :as str]
            [malli.core :as m]
            [moon.component :refer [defsystem defmethods]]
            [moon.val-max :as val-max]))

; TODO assert here also int?

(defsystem value-text)

(defsystem apply [_ base-value])

(defsystem order)

(defmethods :op/inc
  (value-text [[_ value]]
    (str value))

  (apply [[_ value] base-value]
    (+ base-value value))

  (order [_]
    0))

(defmethods :op/mult
  (value-text [[_ value]]
    (str value "%"))

  (apply [[_ value] base-value]
    (* base-value (inc (/ value 100))))

  (order [_]
    1))

(defn- ->pos-int [v]
  (-> v int (max 0)))

(defn- val-max-op-k->parts [op-k]
  (let [[val-or-max inc-or-mult] (mapv keyword (str/split (name op-k) #"-"))]
    [val-or-max (keyword "op" (name inc-or-mult))]))

(comment
 (= (val-max-op-k->parts :op/val-inc) [:val :op/inc])
 )

(defmethods :op/val-max
  (value-text [[op-k value]]
    (let [[val-or-max op-k] (val-max-op-k->parts op-k)]
      (str (value-text [op-k value]) " " (case val-or-max
                                           :val "Minimum"
                                           :max "Maximum"))))


  (apply [[operation-k value] val-max]
    (assert (m/validate val-max/schema val-max) (pr-str val-max))
    (let [[val-or-max op-k] (val-max-op-k->parts operation-k)
          f #(apply [op-k value] %)
          [v mx] (update val-max (case val-or-max :val 0 :max 1) f)
          v  (->pos-int v)
          mx (->pos-int mx)
          vmx (case val-or-max
                :val [v (max v mx)]
                :max [(min v mx) mx])]
      (assert (m/validate val-max/schema vmx))
      vmx))

  (order [[op-k value]]
    (let [[_ op-k] (val-max-op-k->parts op-k)]
      (order [op-k value]))))

(derive :op/val-inc  :op/val-max)
(derive :op/val-mult :op/val-max)
(derive :op/max-inc  :op/val-max)
(derive :op/max-mult :op/val-max)

(comment
 (and
  (= (op/apply [:op/val-inc 30]    [5 10]) [35 35])
  (= (op/apply [:op/max-mult -0.5] [5 10]) [5 5])
  (= (op/apply [:op/val-mult 2]    [5 10]) [15 15])
  (= (op/apply [:op/val-mult 1.3]  [5 10]) [11 11])
  (= (op/apply [:op/max-mult -0.8] [5 10]) [1 1])
  (= (op/apply [:op/max-mult -0.9] [5 10]) [0 0]))
 )
