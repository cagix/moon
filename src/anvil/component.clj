(ns anvil.component
  (:require [clojure.utils :refer [defsystem]]))

(defsystem applicable?)

(defsystem handle)

(defsystem useful?)
(defmethod useful? :default [_ _ctx c] true)

(defsystem render-effect)
(defmethod render-effect :default [_ _ctx c])
