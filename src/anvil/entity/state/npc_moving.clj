(ns ^:no-doc anvil.entity.state.npc-moving
  (:require [anvil.component :as component]
            [anvil.entity :as entity]
            [anvil.entity.stat :as stat]
            [anvil.world :refer [timer stopped?]]
            [gdl.utils :refer [defmethods]]))

(defmethods :npc-moving
  (component/->v [[_ eid movement-vector]]
    {:eid eid
     :movement-vector movement-vector
     :counter (timer (* (stat/->value @eid :entity/reaction-time) 0.016))})

  (component/enter [[_ {:keys [eid movement-vector]}]]
    (swap! eid assoc :entity/movement {:direction movement-vector
                                       :speed (or (stat/->value @eid :entity/movement-speed) 0)}))

  (component/exit [[_ {:keys [eid]}]]
    (swap! eid dissoc :entity/movement))

  (component/tick [[_ {:keys [counter]}] eid]
    (when (stopped? counter)
      (entity/event eid :timer-finished))))
