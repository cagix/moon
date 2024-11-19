(ns moon.player
  (:require [moon.system :refer [defsystem]]
            [moon.entity.fsm :as fsm]
            [moon.world :refer [player-eid]]))

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

(defn- state []
  (fsm/state-obj @player-eid))

(defn state-pauses-game?  []      (pause-game?             (state)))
(defn update-state        []      (manual-tick             (state)))
(defn draw-state          []      (draw-gui-view           (state)))
(defn clicked-inventory   [cell]  (clicked-inventory-cell  (state) cell))
(defn clicked-skillmenu   [skill] (clicked-skillmenu-skill (state) skill))
