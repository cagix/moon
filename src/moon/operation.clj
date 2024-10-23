(ns moon.operation
  (:refer-clojure :exclude [apply])
  (:require [clojure.math :as math]
            [moon.component :refer [defsystem defc]]))

(defsystem value-text)
(defsystem apply [_ base-value])
(defsystem order)

(defn- +? [n]
  (case (math/signum n)
    0.0 ""
    1.0 "+"
    -1.0 ""))

(defn info-text [{value 1 :as operation}]
  (str (+? value) (value-text operation)))

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
