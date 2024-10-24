(ns moon.operation
  (:refer-clojure :exclude [apply])
  (:require [moon.component :refer [defsystem defc]]))

(defsystem value-text)

(defsystem apply [_ base-value])

(defsystem order)

(defc :op/inc
  {:schema number?
   :let value}
  (value-text [_]
    (str value))

  (apply [_ base-value]
    (+ base-value value))

  (order [_] 0))

(defc :op/mult
  {:schema number?
   :let value}
  (value-text [_]
    (str (int (* 100 value)) "%"))

  (apply [_ base-value]
    (* base-value (inc value)))

  (order [_] 1))
