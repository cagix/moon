(ns moon.entity.fsm
  (:require [moon.component :as component]
            [moon.entity :as entity]
            [reduce-fsm :as fsm]))

; fsm throws when initial-state is not part of states, so no need to assert initial-state
; initial state is nil, so associng it. make bug report at reduce-fsm?
(defn- ->init-fsm [fsm initial-state]
  (assoc (fsm initial-state nil) :state initial-state))

(defn- send-event! [eid event params]
  (when-let [fsm (:entity/fsm @eid)]
    (let [old-state-k (:state fsm)
          new-fsm (fsm/fsm-event fsm event)
          new-state-k (:state new-fsm)]
      (when-not (= old-state-k new-state-k)
        (let [old-state-obj (entity/state-obj @eid)
              new-state-obj [new-state-k (entity/->v [new-state-k eid params])]]
          [#(entity/exit old-state-obj)
           #(entity/enter new-state-obj)
           (when (:entity/player? @eid)
             #(entity/player-enter new-state-obj))
           [:e/assoc eid :entity/fsm new-fsm]
           [:e/dissoc eid old-state-k]
           [:e/assoc eid new-state-k (new-state-obj 1)]])))))

(defmethods :entity/fsm
  (entity/create [[k {:keys [fsm initial-state]}] eid]
    [[:e/assoc eid k (->init-fsm (component/create [fsm]) initial-state)]
     [:e/assoc eid initial-state (entity/->v [initial-state eid])]])

  (component/info [[_ fsm]]
    (str "[YELLOW]State: " (name (:state fsm)) "[]"))

  (component/handle [[_ eid event params]]
    (send-event! eid event params)))
