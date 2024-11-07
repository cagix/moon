(ns moon.entity.player.moving
  (:require [gdl.input :refer [WASD-movement-vector]]
            [moon.entity.stat :as stat]))

(defn ->v [eid movement-vector]
  {:eid eid
   :movement-vector movement-vector})

(defn player-enter [_]
  [[:tx/cursor :cursors/walking]])

(defn pause-game? [_]
  false)

(defn enter [{:keys [eid movement-vector]}]
  [[:entity/movement eid {:direction movement-vector
                          :speed (stat/value @eid :stats/movement-speed)}]])

(defn exit [{:keys [eid]}]
  [[:entity/movement eid nil]])

(defn tick [{:keys [movement-vector]} eid]
  (if-let [movement-vector (WASD-movement-vector)]
    [[:entity/movement eid {:direction movement-vector
                            :speed (stat/value @eid :stats/movement-speed)}]]
    [[:entity/fsm eid :no-movement-input]]))
