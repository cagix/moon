(ns moon.systems.component
  (:require [moon.system :refer [defsystem]]))

(defsystem info)
(defmethod info :default [_])
