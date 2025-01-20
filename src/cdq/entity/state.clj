(ns cdq.entity.state
  (:require [cdq.utils :refer [defsystem]]))

(defsystem enter)
(defmethod enter :default [_ c])

(defsystem exit)
(defmethod exit :default [_ c])

(defsystem pause-game?)
(defmethod pause-game? :default [_])

(defsystem clicked-inventory-cell)
(defmethod clicked-inventory-cell :default [_ cell c])

(defsystem clicked-skillmenu-skill)
(defmethod clicked-skillmenu-skill :default [_ skill c])

(defsystem manual-tick)
(defmethod manual-tick :default [_ c])
