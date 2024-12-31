(ns ^:no-doc anvil.entity.state.npc-dead
  (:require [anvil.entity :as entity]
            [clojure.utils :refer [defmethods]]))

(defmethods :npc-dead
  (entity/->v [[_ eid] c]
    {:eid eid})

  (entity/enter [[_ {:keys [eid]}] c]
    (swap! eid assoc :entity/destroyed? true)))
