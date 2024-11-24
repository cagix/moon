(ns moon.systems.entity-state
  (:require [forge.system :refer [defsystem]]))

(defsystem enter)
(defmethod enter :default [_])

(defsystem exit)
(defmethod exit :default [_])

(defsystem cursor)
(defmethod cursor :default [_])

(defsystem pause-game?)
(defmethod pause-game? :default [_])

(defsystem manual-tick)
(defmethod manual-tick :default [_])

(defsystem clicked-inventory-cell [_ cell])
(defmethod clicked-inventory-cell :default [_ cell])

(defsystem clicked-skillmenu-skill [_ skill])
(defmethod clicked-skillmenu-skill :default [_ skill])

(defsystem draw-gui-view [_])
(defmethod draw-gui-view :default [_])
