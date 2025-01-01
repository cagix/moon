(ns ^:no-doc anvil.entity.state.npc-dead
  (:require [anvil.entity :as entity]
            [clojure.component :refer [defcomponent]]))

(defcomponent :npc-dead
  (entity/->v [[_ eid] c]
    {:eid eid})

  (entity/enter [[_ {:keys [eid]}] c]
    (swap! eid assoc :entity/destroyed? true)))
