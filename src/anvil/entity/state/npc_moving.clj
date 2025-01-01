(ns ^:no-doc anvil.entity.state.npc-moving
  (:require [anvil.entity :as entity]
            [cdq.context :refer [timer stopped?]]
            [clojure.component :as component :refer [defcomponent]]))

(defcomponent :npc-moving
  (component/create [[_ eid movement-vector] c]
    {:eid eid
     :movement-vector movement-vector
     :counter (timer c (* (entity/stat @eid :entity/reaction-time) 0.016))})

  (component/enter [[_ {:keys [eid movement-vector]}] c]
    (swap! eid assoc :entity/movement {:direction movement-vector
                                       :speed (or (entity/stat @eid :entity/movement-speed) 0)}))

  (component/exit [[_ {:keys [eid]}] c]
    (swap! eid dissoc :entity/movement))

  (component/tick [[_ {:keys [counter]}] eid c]
    (when (stopped? c counter)
      (entity/event c eid :timer-finished))))
