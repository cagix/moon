(ns cdq.impl.entity
  (:require [cdq.audio.sound :as sound]
            [cdq.db :as db]
            [cdq.entity :as entity]
            [cdq.graphics :as graphics]
            [cdq.graphics.batch :as batch]
            [cdq.timer :as timer]
            [cdq.utils :refer [safe-merge]]
            [cdq.world :refer [delayed-alert
                               spawn-audiovisual
                               show-modal
                               spawn-item
                               item-place-position
                               world-item?]]))

; entity defmethods:
; * cdq.render
; * cdq.widgets.inventory
; * cdq.widgets.skill-window
; * cdq.world (create!)

; TODO - multimethods also to 'cdq.component'

(defmethod entity/create :entity/delete-after-duration
  [[_ duration]
   {:keys [cdq.context/elapsed-time] :as c}]
  (timer/create elapsed-time duration))

(defmethod entity/create :entity/hp
  [[_ v] _c]
  [v v])

(defmethod entity/create :entity/mana
  [[_ v] _c]
  [v v])

(defmethod entity/create :entity/projectile-collision
  [[_ v] c]
  (assoc v :already-hit-bodies #{}))

(defn- apply-action-speed-modifier [entity skill action-time]
  (/ action-time
     (or (entity/stat entity (:skill/action-time-modifier-key skill))
         1)))

(defmethod entity/create :active-skill
  [[_ eid [skill effect-ctx]]
   {:keys [cdq.context/elapsed-time]}]
  {:eid eid
   :skill skill
   :effect-ctx effect-ctx
   :counter (->> skill
                 :skill/action-time
                 (apply-action-speed-modifier @eid skill)
                 (timer/create elapsed-time))})

(defmethod entity/create :npc-dead
  [[_ eid] c]
  {:eid eid})

(defmethod entity/create :npc-idle
  [[_ eid] c]
  {:eid eid})

(defmethod entity/create :npc-moving
  [[_ eid movement-vector]
   {:keys [cdq.context/elapsed-time]}]
  {:eid eid
   :movement-vector movement-vector
   :counter (timer/create elapsed-time (* (entity/stat @eid :entity/reaction-time) 0.016))})

(defmethod entity/create :npc-sleeping
  [[_ eid] c]
  {:eid eid})

(defmethod entity/create :player-dead
  [[k] {:keys [cdq/db] :as c}]
  (db/build db :player-dead/component.enter c))

(defmethod entity/create :player-idle
  [[_ eid] {:keys [cdq/db] :as c}]
  (safe-merge (db/build db :player-idle/clicked-inventory-cell c)
              {:eid eid}))

(defmethod entity/create :player-item-on-cursor
  [[_ eid item] {:keys [cdq/db] :as c}]
  (safe-merge (db/build db :player-item-on-cursor/component c)
              {:eid eid
               :item item}))

(defmethod entity/create :player-moving
  [[_ eid movement-vector] c]
  {:eid eid
   :movement-vector movement-vector})

(defmethod entity/create :stunned
  [[_ eid duration]
   {:keys [cdq.context/elapsed-time]}]
  {:eid eid
   :counter (timer/create elapsed-time duration)})

(defmethod entity/draw-gui-view :player-item-on-cursor
  [[_ {:keys [eid]}] {:keys [cdq.graphics/ui-viewport] :as c}]
  (when (not (world-item? c))
    (batch/draw-centered c
                         (:entity/image (:entity/item-on-cursor @eid))
                         (graphics/mouse-position ui-viewport))))

(def ^:private components
  {:entity/destroy-audiovisual {:destroy! (fn [audiovisuals-id eid {:keys [cdq/db] :as c}]
                                            (spawn-audiovisual c
                                                               (:position @eid)
                                                               (db/build db audiovisuals-id c)))}
   :player-idle           {:pause-game? true}
   :active-skill          {:pause-game? false
                           :cursor :cursors/sandclock
                           :enter (fn [[_ {:keys [eid skill]}]
                                       {:keys [cdq.context/elapsed-time] :as c}]
                                    (sound/play (:skill/start-action-sound skill))
                                    (when (:skill/cooldown skill)
                                      (swap! eid assoc-in
                                             [:entity/skills (:property/id skill) :skill/cooling-down?]
                                             (timer/create elapsed-time (:skill/cooldown skill))))
                                    (when (and (:skill/cost skill)
                                               (not (zero? (:skill/cost skill))))
                                      (swap! eid entity/pay-mana-cost (:skill/cost skill))))}
   :player-dead           {:pause-game? true
                           :cursor :cursors/black-x
                           :enter (fn [[_ {:keys [tx/sound
                                                  modal/title
                                                  modal/text
                                                  modal/button-text]}]
                                       c]
                                    (sound/play sound)
                                    (show-modal c {:title title
                                                   :text text
                                                   :button-text button-text
                                                   :on-click (fn [])}))}
   :player-item-on-cursor {:pause-game? true
                           :cursor :cursors/hand-grab
                           :enter (fn [[_ {:keys [eid item]}] c]
                                    (swap! eid assoc :entity/item-on-cursor item))
                           :exit (fn [[_ {:keys [eid player-item-on-cursor/place-world-item-sound]}] c]
                                   ; at clicked-cell when we put it into a inventory-cell
                                   ; we do not want to drop it on the ground too additonally,
                                   ; so we dissoc it there manually. Otherwise it creates another item
                                   ; on the ground
                                   (let [entity @eid]
                                     (when (:entity/item-on-cursor entity)
                                       (sound/play place-world-item-sound)
                                       (swap! eid dissoc :entity/item-on-cursor)
                                       (spawn-item c
                                                   (item-place-position c entity)
                                                   (:entity/item-on-cursor entity)))))}
   :player-moving         {:pause-game? false
                           :cursor :cursors/walking
                           :enter (fn [[_ {:keys [eid movement-vector]}] c]
                                    (swap! eid assoc :entity/movement {:direction movement-vector
                                                                       :speed (entity/stat @eid :entity/movement-speed)}))
                           :exit (fn [[_ {:keys [eid]}] c]
                                   (swap! eid dissoc :entity/movement))}
   :stunned               {:pause-game? false
                           :cursor :cursors/denied}
   :npc-dead              {:enter (fn [[_ {:keys [eid]}] c]
                                    (swap! eid assoc :entity/destroyed? true))}
   :npc-moving            {:enter (fn [[_ {:keys [eid movement-vector]}] c]
                                    (swap! eid assoc :entity/movement {:direction movement-vector
                                                                       :speed (or (entity/stat @eid :entity/movement-speed) 0)}))
                           :exit (fn [[_ {:keys [eid]}] c]
                                   (swap! eid dissoc :entity/movement))}
   :npc-sleeping          {:exit (fn [[_ {:keys [eid]}] c]
                                   (delayed-alert c
                                                  (:position       @eid)
                                                  (:entity/faction @eid)
                                                  0.2)
                                   (swap! eid entity/add-text-effect c "[WHITE]!"))}})

(defn add-components [context _config]
  (assoc context :context/entity-components components))
