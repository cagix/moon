(ns ^:no-doc core.entity.state.npc-dead
  (:require [core.component :as component :refer [defcomponent]]
            [core.entity.state :as state]))

(defcomponent :npc-dead
  {:let {:keys [eid]}}
  (component/create [[_ eid] _ctx]
    {:eid eid})

  (state/enter [_ _ctx]
    [[:e/destroy eid]]))
