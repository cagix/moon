(ns forge.entity.state.player-moving
  (:require [clojure.utils :refer [defmethods]]
            [forge.app.shape-drawer :as sd]
            [forge.entity :refer [->v tick]]
            [forge.entity.fsm :refer [send-event]]
            [forge.entity.state :refer [enter exit cursor pause-game?]]
            [forge.world.time :refer [timer stopped?]]))

(defmethods :player-moving
  (->v [[_ eid movement-vector]]
    {:eid eid
     :movement-vector movement-vector})

  (cursor [_]
    :cursors/walking)

  (pause-game? [_]
    false)

  (enter [[_ {:keys [eid movement-vector]}]]
    (swap! eid assoc :entity/movement {:direction movement-vector
                                       :speed (stat/->value @eid :entity/movement-speed)}))

  (exit [[_ {:keys [eid]}]]
    (swap! eid dissoc :entity/movement))

  (tick [[_ {:keys [movement-vector]}] eid]
    (if-let [movement-vector (controls/movement-vector)]
      (swap! eid assoc :entity/movement {:direction movement-vector
                                         :speed (stat/->value @eid :entity/movement-speed)})
      (send-event eid :no-movement-input))))
