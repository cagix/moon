(ns moon.entity.fsm
  (:require [gdl.graphics.cursors :as cursors]
            [gdl.system :refer [defsystem *k*]]
            [moon.component :as component]
            [moon.entity :as entity]
            [reduce-fsm :as fsm]))

(defsystem enter)
(defmethod enter :default [_])

(defsystem exit)
(defmethod exit :default [_])

(defsystem cursor)
(defmethod cursor :default [_])

(defn state-k [entity]
  (-> entity :entity/fsm :state))

(defn state-obj [entity]
  (let [k (state-k entity)]
    [k (k entity)]))

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
        (let [old-state-obj (state-obj @eid)
              new-state-obj [new-state-k (entity/->v (if params
                                                       [new-state-k eid params]
                                                       [new-state-k eid]))]]
          (when (:entity/player? @eid)
            (when-let [crs (cursor new-state-obj)]
              (cursors/set crs)))
          [#(exit old-state-obj)
           #(enter new-state-obj)
           [:e/assoc eid :entity/fsm new-fsm]
           [:e/dissoc eid old-state-k]
           [:e/assoc eid new-state-k (new-state-obj 1)]])))))

(defn create [{:keys [fsm initial-state]} eid]
  [[:e/assoc eid *k* (->init-fsm (component/create [fsm]) initial-state)]
   [:e/assoc eid initial-state (entity/->v [initial-state eid])]])

(defn info [fsm]
  (str "[YELLOW]State: " (name (:state fsm)) "[]"))

(defn handle
  ([eid event]
   (send-event! eid event nil))
  ([eid event params]
   (send-event! eid event params)))
