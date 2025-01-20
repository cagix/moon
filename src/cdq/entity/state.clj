(ns cdq.entity.state
  (:require [cdq.utils :refer [defsystem]]))

(defsystem clicked-inventory-cell)
(defmethod clicked-inventory-cell :default [_ cell c])

(defsystem clicked-skillmenu-skill)
(defmethod clicked-skillmenu-skill :default [_ skill c])

(defsystem manual-tick)
(defmethod manual-tick :default [_ c])
