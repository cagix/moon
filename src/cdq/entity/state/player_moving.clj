(ns cdq.entity.state.player-moving
  (:require [cdq.controls :as controls]
            [cdq.entity :as entity]
            [cdq.utils :refer [defmethods]]))

(defn handle-input [eid ctx]
  (if-let [movement-vector (controls/player-movement-vector ctx)]
    [[:tx/set-movement eid movement-vector]]
    [[:tx/event eid :no-movement-input]]))

(defmethods :player-moving
  (entity/create [[_ eid movement-vector] _ctx]
    {:movement-vector movement-vector}))

(defn enter [{:keys [movement-vector]} eid]
  [[:tx/set-movement eid movement-vector]])

(defn exit [_ eid _ctx]
  [[:tx/dissoc eid :entity/movement]])
