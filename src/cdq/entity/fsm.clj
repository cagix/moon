(ns cdq.entity.fsm
  (:require [cdq.entity :as entity]
            [cdq.entity.state :as state]
            [cdq.fsm :as fsm]
            cdq.graphics))

(def state->cursor
  {:active-skill          :cursors/sandclock
   :player-dead           :cursors/black-x
   :player-item-on-cursor :cursors/hand-grab
   :player-moving         :cursors/walking
   :stunned               :cursors/denied})

(defn event
  ([c eid event*]
   (event c eid event* nil))
  ([c eid event params]
   (when-let [fsm (:entity/fsm @eid)]
     (let [old-state-k (:state fsm)
           new-fsm (fsm/event fsm event)
           new-state-k (:state new-fsm)]
       (when-not (= old-state-k new-state-k)
         (let [old-state-obj (entity/state-obj @eid)
               new-state-obj [new-state-k (entity/create (if params
                                                           [new-state-k eid params]
                                                           [new-state-k eid])
                                                         c)]]
           (when (:entity/player? @eid)
             (when-let [cursor (state->cursor new-state-k)]
               (cdq.graphics/set-cursor c cursor)))
           (swap! eid #(-> %
                           (assoc :entity/fsm new-fsm
                                  new-state-k (new-state-obj 1))
                           (dissoc old-state-k)))
           (state/exit  old-state-obj c)
           (state/enter new-state-obj c)))))))
