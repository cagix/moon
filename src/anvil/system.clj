(ns anvil.system
  (:require [clojure.utils :refer [defsystem]]))

(defsystem ->v)
(defmethod ->v :default [[_ v]]
  v)

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

(defsystem enter)
(defmethod enter :default [_])

(defsystem exit)
(defmethod exit :default [_])

(defsystem cursor)
(defmethod cursor :default [_])

(defsystem manual-tick)
(defmethod manual-tick :default [_])

(defsystem pause-game?)
(defmethod pause-game? :default [_])

(defsystem draw-gui-view [_])
(defmethod draw-gui-view :default [_])

(defsystem clicked-inventory-cell [_ cell])
(defmethod clicked-inventory-cell :default [_ cell])

(defsystem clicked-skillmenu-skill [_ skill])
(defmethod clicked-skillmenu-skill :default [_ skill])

(defsystem handle [_ ctx])

(defsystem applicable? [_ ctx])

(defsystem useful? [_  ctx])
(defmethod useful? :default [_ _ctx] true)

(defsystem render-effect [_  ctx])
(defmethod render-effect :default  [_ _ctx])
