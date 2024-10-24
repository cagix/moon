(ns ^:no-doc moon.creature.player.moving
  (:require [gdl.input :refer [WASD-movement-vector]]
            [moon.component :refer [defc]]
            [moon.entity :as entity]
            [moon.entity.state :as state]
            [moon.entity.modifiers :refer [entity-stat]]))

(defc :player-moving
  {:let {:keys [eid movement-vector]}}
  (entity/->v [[_ eid movement-vector]]
    {:eid eid
     :movement-vector movement-vector})

  (state/player-enter [_]
    [[:tx/cursor :cursors/walking]])

  (state/pause-game? [_]
    false)

  (state/enter [_]
    [[:tx/set-movement eid {:direction movement-vector
                            :speed (entity-stat @eid :stats/movement-speed)}]])

  (state/exit [_]
    [[:tx/set-movement eid nil]])

  (entity/tick [_ eid]
    (if-let [movement-vector (WASD-movement-vector)]
      [[:tx/set-movement eid {:direction movement-vector
                              :speed (entity-stat @eid :stats/movement-speed)}]]
      [[:tx/event eid :no-movement-input]])))
