(ns components.entity-state.player-moving
  (:require [utils.wasd-movement :refer [WASD-movement-vector]]
            [core.component :as component :refer [defcomponent]]
            [core.entity :as entity]
            [core.entity-state :as state]))

(defcomponent :player-moving {}
  {:keys [eid movement-vector]}
  (component/create [[_ eid movement-vector] _ctx]
    {:eid eid
     :movement-vector movement-vector})

  (state/player-enter [_]
    [[:tx.context.cursor/set :cursors/walking]])

  (state/pause-game? [_]
    false)

  (state/enter [_ _ctx]
    [[:tx.entity/set-movement eid {:direction movement-vector
                                   :speed (entity/stat @eid :stats/movement-speed)}]])

  (state/exit [_ _ctx]
    [[:tx.entity/set-movement eid nil]])

  (state/tick [_ context]
    (if-let [movement-vector (WASD-movement-vector)]
      [[:tx.entity/set-movement eid {:direction movement-vector
                                     :speed (entity/stat @eid :stats/movement-speed)}]]
      [[:tx/event eid :no-movement-input]])))