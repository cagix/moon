(ns cdq.entity.state.player-moving
  (:require [cdq.controls :as controls]
            [cdq.entity :as entity]
            [cdq.state :as state]
            [clojure.utils :refer [defcomponent]]))

(defcomponent :player-moving
  (entity/create [[_ eid movement-vector] _ctx]
    {:movement-vector movement-vector})

  (entity/tick! [[_ {:keys [movement-vector]}] eid ctx]
    (if-let [movement-vector (controls/player-movement-vector ctx)]
      [[:tx/set-movement eid movement-vector]]
      [[:tx/event eid :no-movement-input]]))

  (state/cursor [_] :cursors/walking)

  (state/pause-game? [_] false)

  (state/enter! [[_ {:keys [movement-vector]}] eid]
    [[:tx/set-movement eid movement-vector]])

  (state/exit! [_ eid _ctx]
    [[:tx/dissoc eid :entity/movement]]))
