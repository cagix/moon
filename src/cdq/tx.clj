(ns cdq.tx
  (:require [cdq.effect :as effect]
            [cdq.effect-context :as effect-context]
            [cdq.entity :as entity]
            [cdq.fsm :as fsm]
            [cdq.utils :as utils])
  (:import (com.badlogic.gdx Gdx)))

(defn cursor [{:keys [cdq.graphics/cursors]} cursor-key]
  (.setCursor Gdx/graphics (utils/safe-get cursors cursor-key)))

(defn event
  ([context eid event*]
   (event context eid event* nil))
  ([context eid event params]
   (when-let [fsm (:entity/fsm @eid)]
     (let [old-state-k (:state fsm)
           new-fsm (fsm/event fsm event)
           new-state-k (:state new-fsm)]
       (when-not (= old-state-k new-state-k)
         (let [old-state-obj (entity/state-obj @eid)
               new-state-obj [new-state-k (entity/create (if params
                                                           [new-state-k eid params]
                                                           [new-state-k eid])
                                                         context)]
               entity-states (:context/entity-components context)]
           (when (:entity/player? @eid)
             (when-let [cursor-key (get-in entity-states [new-state-k :cursor])]
               (cursor context cursor-key)))
           (swap! eid #(-> %
                           (assoc :entity/fsm new-fsm
                                  new-state-k (new-state-obj 1))
                           (dissoc old-state-k)))
           (when-let [f (get-in entity-states [old-state-k :exit])]
             (f old-state-obj context))
           (when-let [f (get-in entity-states [new-state-k :enter])]
             (f new-state-obj context))))))))

(defn effect [context effect-ctx effect]
  (run! #(effect/handle % effect-ctx context)
        (effect-context/filter-applicable? effect-ctx effect)))
