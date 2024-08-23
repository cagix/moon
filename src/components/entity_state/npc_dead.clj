(ns components.entity-state.npc-dead
  (:require [core.component :as component :refer [defcomponent]]
            [core.entity-state :as state]))

(defcomponent :npc-dead {}
  {:keys [eid]}
  (component/create [[_ eid] _ctx]
    {:eid eid})

  (state/enter [_ _ctx]
    [[:tx/destroy eid]]))