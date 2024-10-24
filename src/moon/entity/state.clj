(ns ^:no-doc moon.entity.state
  (:require [moon.component :refer [defsystem defc] :as component]
            [moon.entity :as entity]
            [reduce-fsm :as fsm]))

(defsystem enter)
(defmethod enter :default [_])

(defsystem exit)
(defmethod exit :default [_])

(defsystem player-enter)
(defmethod player-enter :default [_])

(defsystem pause-game?)
(defmethod pause-game? :default [_])

(defsystem manual-tick)
(defmethod manual-tick :default [_])

(defsystem clicked-inventory-cell [_ cell])
(defmethod clicked-inventory-cell :default [_ cell])

(defsystem clicked-skillmenu-skill [_ skill])
(defmethod clicked-skillmenu-skill :default [_ skill])

; fsm throws when initial-state is not part of states, so no need to assert initial-state
; initial state is nil, so associng it. make bug report at reduce-fsm?
(defn- ->init-fsm [fsm initial-state]
  (assoc (fsm initial-state nil) :state initial-state))

(defc :entity/state
  (entity/create [[k {:keys [fsm initial-state]}] eid]
    [[:e/assoc eid k (->init-fsm (component/create [fsm nil]) initial-state)]
     [:e/assoc eid initial-state (entity/->v [initial-state eid])]])

  (component/info [[_ fsm]]
    (str "[YELLOW]State: " (name (:state fsm)) "[]")))

(defn state-k [entity]
  (-> entity :entity/state :state))

(defn state-obj [entity]
  (let [k (state-k entity)]
    [k (k entity)]))

(defn- send-event! [eid event params]
  (when-let [fsm (:entity/state @eid)]
    (let [old-state-k (:state fsm)
          new-fsm (fsm/fsm-event fsm event)
          new-state-k (:state new-fsm)]
      (when-not (= old-state-k new-state-k)
        (let [old-state-obj (state-obj @eid)
              new-state-obj [new-state-k (entity/->v [new-state-k eid params])]]
          [#(exit old-state-obj)
           #(enter new-state-obj)
           (when (:entity/player? @eid) #(player-enter new-state-obj))
           [:e/assoc eid :entity/state new-fsm]
           [:e/dissoc eid old-state-k]
           [:e/assoc eid new-state-k (new-state-obj 1)]])))))

(defc :tx/event
  (component/handle [[_ eid event params]]
    (send-event! eid event params)))
