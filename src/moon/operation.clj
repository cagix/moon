(ns moon.operation
  (:refer-clojure :exclude [apply]))

(defsystem value-text)

(defsystem apply [_ base-value])

(defsystem order)
