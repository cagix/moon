(ns ^:no-doc anvil.entity.state.player-moving
  (:require [anvil.component :as component]
            [anvil.controls :as controls]
            [anvil.entity :as entity]))

(defmethods :player-moving
  (component/->v [[_ eid movement-vector]]
    {:eid eid
     :movement-vector movement-vector})

  (component/enter [[_ {:keys [eid movement-vector]}]]
    (swap! eid assoc :entity/movement {:direction movement-vector
                                       :speed (entity/stat @eid :entity/movement-speed)}))

  (component/exit [[_ {:keys [eid]}]]
    (swap! eid dissoc :entity/movement))

  (component/tick [[_ {:keys [movement-vector]}] eid]
    (if-let [movement-vector (controls/movement-vector)]
      (swap! eid assoc :entity/movement {:direction movement-vector
                                         :speed (entity/stat @eid :entity/movement-speed)})
      (entity/event eid :no-movement-input))))


