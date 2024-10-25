(ns moon.operation
  (:refer-clojure :exclude [apply])
  (:require [moon.component :refer [defsystem]]))

(defsystem value-text)

(defsystem apply [_ base-value])

(defsystem order)
