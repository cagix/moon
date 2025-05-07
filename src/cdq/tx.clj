(ns cdq.tx
  (:require [cdq.entity :as entity]
            [cdq.entity.state :as state]
            [cdq.graphics :as graphics]
            [cdq.timer :as timer]
            [cdq.ui :as ui]
            [cdq.ui.action-bar :as action-bar]
            [cdq.ui.stage :as stage]
            [reduce-fsm :as fsm])
  (:import (com.badlogic.gdx.scenes.scene2d Actor)))

(defn event
  ([eid event*]
   (event eid event* nil))
  ([eid event params]
   (when-let [fsm (:entity/fsm @eid)]
     (let [old-state-k (:state fsm)
           new-fsm (fsm/fsm-event fsm event)
           new-state-k (:state new-fsm)]
       (when-not (= old-state-k new-state-k)
         (let [old-state-obj (entity/state-obj @eid)
               new-state-obj [new-state-k (entity/create (if params
                                                           [new-state-k eid params]
                                                           [new-state-k eid]))]]
           (when (:entity/player? @eid)
             (when-let [cursor-key (state/cursor new-state-obj)]
               (graphics/set-cursor! cursor-key)))
           (swap! eid #(-> %
                           (assoc :entity/fsm new-fsm
                                  new-state-k (new-state-obj 1))
                           (dissoc old-state-k)))
           (state/exit!  old-state-obj)
           (state/enter! new-state-obj)))))))

; we cannot just set/unset movement direction
; because it is handled by the state enter/exit for npc/player movement state ...
; so we cannot expose it as a 'transaction'
; so the movement should be updated in the respective npc/player movement 'state' and no movement 'component' necessary !
; for projectiles inside projectile update !?
(defn set-movement [eid movement-vector]
  (swap! eid assoc :entity/movement {:direction movement-vector
                                     :speed (or (entity/stat @eid :entity/movement-speed) 0)}))

(defn mark-destroyed [eid]
  (swap! eid assoc :entity/destroyed? true))

(defn toggle-inventory-window []
  (ui/toggle-visible! (stage/get-inventory)))

; no window movable type cursor appears here like in player idle
; inventory still working, other stuff not, because custom listener to keypresses ? use actor listeners?
; => input events handling
; hmmm interesting ... can disable @ item in cursor  / moving / etc.
(defn show-modal [{:keys [title text button-text on-click]}]
  (assert (not (stage/get-actor ::modal)))
  (stage/add-actor (ui/window {:title title
                               :rows [[(ui/label text)]
                                      [(ui/text-button button-text
                                                       (fn []
                                                         (Actor/.remove (stage/get-actor ::modal))
                                                         (on-click)))]]
                               :id ::modal
                               :modal? true
                               :center-position [(/ (:width  graphics/ui-viewport) 2)
                                                 (* (:height graphics/ui-viewport) (/ 3 4))]
                               :pack? true})))

(defn add-skill [eid {:keys [property/id] :as skill}]
  {:pre [(not (entity/has-skill? @eid skill))]}
  (when (:entity/player? @eid)
    (action-bar/add-skill! (stage/get-action-bar) skill))
  (swap! eid assoc-in [:entity/skills id] skill))

(defn remove-skill [eid {:keys [property/id] :as skill}]
  {:pre [(entity/has-skill? @eid skill)]}
  (when (:entity/player? @eid)
    (action-bar/remove-skill! (stage/get-action-bar) skill))
  (swap! eid update :entity/skills dissoc id))

(defn- add-text-effect [entity text]
  (assoc entity
         :entity/string-effect
         (if-let [string-effect (:entity/string-effect entity)]
           (-> string-effect
               (update :text str "\n" text)
               (update :counter timer/reset))
           {:text text
            :counter (timer/create 0.4)})))

(defn text-effect [eid text]
  (swap! eid add-text-effect text))
