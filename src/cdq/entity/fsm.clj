(ns cdq.entity.fsm
  (:require [cdq.entity.state :as state]))

(defn create!
  [{:keys [fsm initial-state]}
   eid
   {:keys [ctx/fsms]
    :as ctx}]
  ; fsm throws when initial-state is not part of states, so no need to assert initial-state
  ; initial state is nil, so associng it. make bug report at reduce-fsm?
  [[:tx/assoc eid :entity/fsm (assoc ((get fsms fsm) initial-state nil) :state initial-state)]
   [:tx/assoc eid initial-state (state/create ctx initial-state eid nil)]])

(defn info-text [[_ fsm] _ctx]
  (str "State: " (name (:state fsm))))
