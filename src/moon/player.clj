(ns moon.player
  (:require [moon.entity.fsm :as fsm]
            [moon.systems.entity-state :as state]
            [moon.world :refer [player-eid]]))

(defn- state []
  (fsm/state-obj @player-eid))

(defn state-pauses-game?  []      (state/pause-game?             (state)))
(defn update-state        []      (state/manual-tick             (state)))
(defn draw-state          []      (state/draw-gui-view           (state)))
(defn clicked-inventory   [cell]  (state/clicked-inventory-cell  (state) cell))
(defn clicked-skillmenu   [skill] (state/clicked-skillmenu-skill (state) skill))
