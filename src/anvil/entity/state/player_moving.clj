(ns ^:no-doc anvil.entity.state.player-moving
  (:require [anvil.controls :as controls]
            [anvil.entity :as entity]
            [clojure.component :refer [defcomponent]]))

(defcomponent :player-moving
  (entity/->v [[_ eid movement-vector] c]
    {:eid eid
     :movement-vector movement-vector})

  (entity/enter [[_ {:keys [eid movement-vector]}] c]
    (swap! eid assoc :entity/movement {:direction movement-vector
                                       :speed (entity/stat @eid :entity/movement-speed)}))

  (entity/exit [[_ {:keys [eid]}] c]
    (swap! eid dissoc :entity/movement))

  (entity/tick [[_ {:keys [movement-vector]}] eid c]
    (if-let [movement-vector (controls/movement-vector c)]
      (swap! eid assoc :entity/movement {:direction movement-vector
                                         :speed (entity/stat @eid :entity/movement-speed)})
      (entity/event c eid :no-movement-input))))


