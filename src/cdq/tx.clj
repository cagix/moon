(ns cdq.tx
  (:require [cdq.assets :as assets]
            [cdq.audio.sound :as sound]
            [cdq.effect :as effect]
            [cdq.effect-context :as effect-context]
            [cdq.entity :as entity]
            [cdq.fsm :as fsm]
            [cdq.info :as info]
            [cdq.timer :as timer]
            [cdq.ui :as ui]
            [cdq.ui.actor :as actor]
            [cdq.ui.group :as group]
            [cdq.ui.stage :as stage]
            [cdq.utils :as utils])
  (:import (com.badlogic.gdx Gdx)
           (com.badlogic.gdx.scenes.scene2d.ui Button ButtonGroup)))

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

(defn toggle-inventory-window [{:keys [cdq.context/stage]}]
  (actor/toggle-visible! (stage/get-inventory stage)))

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

(defn- action-bar-add-skill [{:keys [cdq.context/stage]}
                             {:keys [property/id entity/image] :as skill}]
  (let [{:keys [horizontal-group button-group]} (stage/get-action-bar stage)
        button (ui/image-button image (fn []) {:scale 2})]
    (actor/set-id button id)
    (ui/add-tooltip! button #(info/text % skill)) ; (assoc ctx :effect/source (world/player)) FIXME
    (group/add-actor! horizontal-group button)
    (ButtonGroup/.add button-group ^Button button)
    nil))

(defn- action-bar-remove-skill [{:keys [cdq.context/stage]}
                                {:keys [property/id]}]
  (let [{:keys [horizontal-group button-group]} (stage/get-action-bar stage)
        button (get horizontal-group id)]
    (actor/remove button)
    (ButtonGroup/.remove button-group ^Button button)
    nil))

(defn add-skill [c eid {:keys [property/id] :as skill}]
  {:pre [(not (entity/has-skill? @eid skill))]}
  (when (:entity/player? @eid)
    (action-bar-add-skill c skill))
  (swap! eid assoc-in [:entity/skills id] skill))

(defn remove-skill [c eid {:keys [property/id] :as skill}]
  {:pre [(entity/has-skill? @eid skill)]}
  (when (:entity/player? @eid)
    (action-bar-remove-skill c skill))
  (swap! eid update :entity/skills dissoc id))

(defn- add-text-effect [entity {:keys [cdq.context/elapsed-time]} text]
  (assoc entity
         :entity/string-effect
         (if-let [string-effect (:entity/string-effect entity)]
           (-> string-effect
               (update :text str "\n" text)
               (update :counter #(timer/reset % elapsed-time)))
           {:text text
            :counter (timer/create elapsed-time 0.4)})))

(defn text-effect [context eid text]
  (swap! eid add-text-effect context text))
