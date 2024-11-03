(ns moon.entity.player.moving
  (:require [gdl.input :refer [WASD-movement-vector]]
            [moon.modifiers :as mods]))

(defn ->v [[_ eid movement-vector]]
  {:eid eid
   :movement-vector movement-vector})

(defn player-enter [_]
  [[:tx/cursor :cursors/walking]])

(defn pause-game? [_]
  false)

(defn enter [[_ {:keys [eid movement-vector]}]]
  [[:entity/movement eid {:direction movement-vector
                          :speed (mods/value @eid :stats/movement-speed)}]])

(defn exit [[_ {:keys [eid]}]]
  [[:entity/movement eid nil]])

(defn tick [[_ {:keys [movement-vector]}] eid]
  (if-let [movement-vector (WASD-movement-vector)]
    [[:entity/movement eid {:direction movement-vector
                            :speed (mods/value @eid :stats/movement-speed)}]]
    [[:entity/fsm eid :no-movement-input]]))
