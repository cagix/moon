(ns moon.entity.player.moving
  (:require [gdl.input :refer [WASD-movement-vector]]
            [moon.entity :as entity]))

(defmethods :player-moving
  {:let {:keys [eid movement-vector]}}
  (entity/->v [[_ eid movement-vector]]
    {:eid eid
     :movement-vector movement-vector})

  (entity/player-enter [_]
    [[:tx/cursor :cursors/walking]])

  (entity/pause-game? [_]
    false)

  (entity/enter [_]
    [[:entity/movement eid {:direction movement-vector
                            :speed (entity/stat @eid :stats/movement-speed)}]])

  (entity/exit [_]
    [[:entity/movement eid nil]])

  (entity/tick [_ eid]
    (if-let [movement-vector (WASD-movement-vector)]
      [[:entity/movement eid {:direction movement-vector
                              :speed (entity/stat @eid :stats/movement-speed)}]]
      [[:entity/fsm eid :no-movement-input]])))
