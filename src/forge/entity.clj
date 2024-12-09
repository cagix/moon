(ns forge.entity
  (:require [clojure.utils :refer [defsystem]]))

(defsystem create [_ eid])
(defmethod create :default [_ eid])

(defsystem destroy [_ eid])
(defmethod destroy :default [_ eid])

(defsystem tick [_ eid])
(defmethod tick :default [_ eid])

(defsystem render-below [_ entity])
(defmethod render-below :default [_ entity])

(defsystem render-default [_ entity])
(defmethod render-default :default [_ entity])

(defsystem render-above [_ entity])
(defmethod render-above :default [_ entity])

(defsystem render-info [_ entity])
(defmethod render-info :default [_ entity])
