(ns anvil.fsm
  (:require [anvil.audio :refer [play-sound]]
            [anvil.entity :as entity]
            [anvil.graphics :as g]
            [anvil.item-on-cursor :refer [item-place-position]]
            [anvil.screen :as screen]
            [anvil.stage :refer [show-modal]]
            [anvil.stat :as stat]
            [clojure.component :refer [defsystem]]
            [clojure.utils :refer [defmethods]]
            [reduce-fsm :as fsm]))

(defn state-k [entity]
  (-> entity :entity/fsm :state))

(defn state-obj [entity]
  (let [k (state-k entity)]
    [k (k entity)]))

(defsystem enter)
(defmethod enter :default [_])

(defsystem exit)
(defmethod exit :default [_])

(defmethod enter :player-dead [_]
  (play-sound "bfxr_playerdeath")
  (show-modal {:title "YOU DIED"
               :text "\nGood luck next time"
               :button-text ":("
               :on-click #(screen/change :screens/main-menu)}))

(defmethods :player-moving
  (enter [[_ {:keys [eid movement-vector]}]]
    (swap! eid assoc :entity/movement {:direction movement-vector
                                       :speed (stat/->value @eid :entity/movement-speed)}))

  (exit [[_ {:keys [eid]}]]
    (swap! eid dissoc :entity/movement)))

(defmethods :player-item-on-cursor
  (enter [[_ {:keys [eid item]}]]
    (swap! eid assoc :entity/item-on-cursor item))

  (exit [[_ {:keys [eid]}]]
    ; at clicked-cell when we put it into a inventory-cell
    ; we do not want to drop it on the ground too additonally,
    ; so we dissoc it there manually. Otherwise it creates another item
    ; on the ground
    (let [entity @eid]
      (when (:entity/item-on-cursor entity)
        (play-sound "bfxr_itemputground")
        (swap! eid dissoc :entity/item-on-cursor)
        (entity/item (item-place-position entity)
                     (:entity/item-on-cursor entity))))))

(defsystem cursor)
(defmethod cursor :default [_])

(defmethod cursor :stunned               [_] :cursors/denied)
(defmethod cursor :player-moving         [_] :cursors/walking)
(defmethod cursor :player-item-on-cursor [_] :cursors/hand-grab)
(defmethod cursor :player-dead           [_] :cursors/black-x)

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
            (when-let [cursor-k (cursor new-state-obj)]
              (g/set-cursor cursor-k)))
          (swap! eid #(-> %
                          (assoc :entity/fsm new-fsm
                                 new-state-k (new-state-obj 1))
                          (dissoc old-state-k)))
          (exit old-state-obj)
          (enter new-state-obj))))))

(defn event
  ([eid event]
   (send-event! eid event nil))
  ([eid event params]
   (send-event! eid event params)))
