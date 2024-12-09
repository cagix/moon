(ns anvil.entity
  (:require [anvil.graphics :refer [set-cursor]]
            [clojure.gdx.math.shapes :as shape]
            [clojure.gdx.math.vector2 :as v]
            [clojure.utils :refer [->tile defsystem]]
            [reduce-fsm :as fsm]
            [forge.modifiers :refer [apply-max-modifier]]))

(defn direction [entity other-entity]
  (v/direction (:position entity) (:position other-entity)))

(defn collides? [entity other-entity]
  (shape/overlaps? entity other-entity))

(defn tile [entity]
  (->tile (:position entity)))

(defn enemy [{:keys [entity/faction]}]
  (case faction
    :evil :good
    :good :evil))

(defn state-k [entity]
  (-> entity :entity/fsm :state))

(defn state-obj [entity]
  (let [k (state-k entity)]
    [k (k entity)]))

(defsystem enter)
(defmethod enter :default [_])

(defsystem exit)
(defmethod exit :default [_])

(defsystem cursor)
(defmethod cursor :default [_])

(defsystem ->v)
(defmethod ->v :default [[_ v]]
  v)

(defn- send-event! [eid event params]
  (when-let [fsm (:entity/fsm @eid)]
    (let [old-state-k (:state fsm)
          new-fsm (fsm/fsm-event fsm event)
          new-state-k (:state new-fsm)]
      (when-not (= old-state-k new-state-k)
        (let [old-state-obj (state-obj @eid)
              new-state-obj [new-state-k (->v (if params
                                                [new-state-k eid params]
                                                [new-state-k eid]))]]
          (when (:entity/player? @eid)
            (when-let [cursor-k (cursor new-state-obj)]
              (set-cursor cursor-k)))
          (swap! eid #(-> %
                          (assoc :entity/fsm new-fsm
                                 new-state-k (new-state-obj 1))
                          (dissoc old-state-k)))
          (exit old-state-obj)
          (enter new-state-obj))))))

(defn send-event
  ([eid event]
   (send-event! eid event nil))
  ([eid event params]
   (send-event! eid event params)))

(defn hitpoints
  "Returns the hitpoints val-max vector `[current-value maximum]` of entity after applying max-hp modifier.
  Current-hp is capped by max-hp."
  [entity]
  (-> entity
      :entity/hp
      (apply-max-modifier entity :modifier/hp-max)))
