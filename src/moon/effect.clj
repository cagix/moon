(ns moon.effect
  (:require [gdl.system :refer [defsystem]]))

(defsystem handle)

(defsystem applicable?)

(defsystem useful?)
(defmethod useful? :default [_] true)

(defsystem render)
(defmethod render :default [_])
