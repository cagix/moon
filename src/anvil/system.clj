(ns anvil.system
  (:require [clojure.utils :refer [defsystem]]))

(defsystem ->v)
(defmethod ->v :default [[_ v]]
  v)

(defsystem create)
(defmethod create :default [_ eid])

(defsystem destroy)
(defmethod destroy :default [_ eid])

(defsystem tick)
(defmethod tick :default [_ eid])

(defsystem render-below)
(defmethod render-below :default [_ entity])

(defsystem render-default)
(defmethod render-default :default [_ entity])

(defsystem render-above)
(defmethod render-above :default [_ entity])

(defsystem render-info)
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

(defsystem clicked-inventory-cell)
(defmethod clicked-inventory-cell :default [_ cell])

(defsystem clicked-skillmenu-skill)
(defmethod clicked-skillmenu-skill :default [_ skill])

(defsystem handle)

(defsystem applicable?)

(defsystem useful?)
(defmethod useful? :default [_ _ctx] true)

(defsystem render-effect)
(defmethod render-effect :default  [_ _ctx])
