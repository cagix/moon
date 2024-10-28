(ns moon.operation.mult
  (:require [moon.operation :as op]))

(defc :op/mult
  {:schema number?
   :let value}
  (op/value-text [_]
    (str (int (* 100 value)) "%"))

  (op/apply [_ base-value]
    (* base-value (inc value)))

  (op/order [_] 1))
