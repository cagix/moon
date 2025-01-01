(ns ^:no-doc anvil.entity.state.npc-dead
  (:require [clojure.component :as component :refer [defcomponent]]))

(defcomponent :npc-dead
  (component/create [[_ eid] c]
    {:eid eid})

  (component/enter [[_ {:keys [eid]}] c]
    (swap! eid assoc :entity/destroyed? true)))
