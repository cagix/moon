(ns ^:no-doc anvil.entity.state.npc-dead
  (:require [anvil.component :as component]
            [gdl.utils :refer [defmethods]]))

(defmethods :npc-dead
  (component/->v [[_ eid]]
    {:eid eid})

  (component/enter [[_ {:keys [eid]}]]
    (swap! eid assoc :entity/destroyed? true)))
