(ns cdq.effect
  (:require [gdl.component :refer [defsystem]]))

(defsystem applicable?)

(defsystem handle)

(defsystem useful?)
(defmethod useful? :default [_ _effect-ctx context] true)

(defsystem render)
(defmethod render :default [_ _effect-ctx context])
