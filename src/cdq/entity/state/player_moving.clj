(ns cdq.entity.state.player-moving
  (:require [cdq.controls :as controls]))

(defn handle-input [eid ctx]
  (if-let [movement-vector (controls/player-movement-vector ctx)]
    [[:tx/set-movement eid movement-vector]]
    [[:tx/event eid :no-movement-input]]))

(defn create [eid movement-vector _ctx]
  {:movement-vector movement-vector})

(defn enter [{:keys [movement-vector]} eid]
  [[:tx/set-movement eid movement-vector]])

(defn exit [_ eid _ctx]
  [[:tx/dissoc eid :entity/movement]])
