(ns moon.effect
  (:require [gdl.system :refer [defsystem]]))

(defsystem handle [_ ctx])

(defsystem applicable? [_ ctx])

(defsystem useful?          [_  ctx])
(defmethod useful? :default [_ _ctx] true)

(defsystem render           [_  ctx])
(defmethod render :default  [_ _ctx])
