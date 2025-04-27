(ns cdq.tx
  (:require [cdq.assets :as assets]
            [cdq.audio.sound :as sound]
            [cdq.effect :as effect]
            [cdq.effect-context :as effect-context]
            [cdq.entity :as entity]
            [cdq.fsm :as fsm]
            [cdq.ui :as ui]
            [cdq.ui.actor :as actor]
            [cdq.ui.stage :as stage]
            [cdq.utils :as utils]
            [cdq.world :refer [get-inventory]])
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

(defn sound [{:keys [cdq/assets]} sound-name]
  (sound/play (assets/sound assets sound-name)))

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

(defn show-player-msg [{:keys [cdq.context/player-message]} text]
  (swap! player-message assoc :text text :counter 0))

(defn toggle-inventory-window [context]
  (actor/toggle-visible! (get-inventory context)))

; no window movable type cursor appears here like in player idle
; inventory still working, other stuff not, because custom listener to keypresses ? use actor listeners?
; => input events handling
; hmmm interesting ... can disable @ item in cursor  / moving / etc.
(defn show-modal [{:keys [cdq.graphics/ui-viewport
                          cdq.context/stage]}
                  {:keys [title text button-text on-click]}]
  (assert (not (::modal stage)))
  (stage/add-actor stage
                   (ui/window {:title title
                               :rows [[(ui/label text)]
                                      [(ui/text-button button-text
                                                       (fn []
                                                         (actor/remove (::modal stage))
                                                         (on-click)))]]
                               :id ::modal
                               :modal? true
                               :center-position [(/ (:width  ui-viewport) 2)
                                                 (* (:height ui-viewport) (/ 3 4))]
                               :pack? true})))
