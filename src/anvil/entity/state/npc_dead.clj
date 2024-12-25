(ns ^:no-doc anvil.entity.state.npc-dead
  (:require [anvil.component :as component]))

(defmethods :npc-dead
  (component/->v [[_ eid] c]
    {:eid eid})

  (component/enter [[_ {:keys [eid]}] c]
    (swap! eid assoc :entity/destroyed? true)))
