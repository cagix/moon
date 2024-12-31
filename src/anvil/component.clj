(ns anvil.component
  (:require [clojure.utils :refer [defsystem]])
  (:refer-clojure :exclude [apply]))

(defsystem info)
(defmethod info :default [_ c])

;; Entity

(defsystem ->v)
(defmethod ->v :default [[_ v] _c]
  v)

(defsystem create)
(defmethod create :default [_ eid c])

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

;; Entity State

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

(defsystem pause-game?)
(defmethod pause-game? :default [_])

;; Effect

(defsystem applicable?)

(defsystem handle)

(defsystem useful?)
(defmethod useful? :default [_ _ctx c] true)

(defsystem render-effect)
(defmethod render-effect :default [_ _ctx c])

;; Operation

(defsystem apply)
(defsystem order)
(defsystem value-text)
