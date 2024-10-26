(ns moon.operation
  (:refer-clojure :exclude [apply])
  (:require [moon.component :refer [defsystem]]))

(defsystem value-text)

(defsystem apply [_ base-value])

(defsystem order)

; now I get it
; moon.operations
; and op/ is a component
; so the systems all to component (anyway)
