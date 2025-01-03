(ns cdq.component
  (:require [clojure.component :refer [defsystem]]))

(defsystem create)
(defmethod create :default [[_ v] _context]
  v)

(defsystem create!)
(defmethod create! :default [_ eid c])

(defsystem destroy)
(defmethod destroy :default [_ eid c])

(defsystem tick)
(defmethod tick :default [_ eid c])

(defsystem render-below)
(defmethod render-below :default [_ entity c])

(defsystem render-default)
(defmethod render-default :default [_ entity c])

(defsystem render-above)
(defmethod render-above :default [_ entity c])

(defsystem render-info)
(defmethod render-info :default [_ entity c])

(defsystem enter)
(defmethod enter :default [_ c])

(defsystem exit)
(defmethod exit :default [_ c])

(defsystem cursor)
(defmethod cursor :default [_])

(defsystem clicked-inventory-cell)
(defmethod clicked-inventory-cell :default [_ cell c])

(defsystem clicked-skillmenu-skill)
(defmethod clicked-skillmenu-skill :default [_ skill c])

(defsystem draw-gui-view)
(defmethod draw-gui-view :default [_ c])

(defsystem manual-tick)
(defmethod manual-tick :default [_ c])

(defsystem apply)
(defsystem order)
(defsystem value-text)
