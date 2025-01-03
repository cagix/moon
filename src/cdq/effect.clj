(ns cdq.effect
  (:require [gdl.utils :refer [defsystem]]))

(defsystem applicable?)

(defsystem handle)

(defsystem useful?)
(defmethod useful? :default [_ _effect-ctx context] true)

(defsystem render)
(defmethod render :default [_ _effect-ctx context])
