(ns anvil.component
  (:require [clojure.utils :refer [defsystem]])
  (:refer-clojure :exclude [apply]))

;; Effect

(defsystem applicable?)

(defsystem handle)

(defsystem useful?)
(defmethod useful? :default [_ _ctx c] true)

(defsystem render-effect)
(defmethod render-effect :default [_ _ctx c])

;; Operation

(defsystem apply)
(defsystem order)
(defsystem value-text)
