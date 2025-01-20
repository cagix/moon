(ns cdq.create.entity.state
  (:require [cdq.audio :as audio]
            [cdq.utils :refer [defcomponent]]
            [cdq.timer :as timer]
            [cdq.entity :as entity]
            [cdq.entity.fsm :as fsm]
            [cdq.entity.state :as state]
            [cdq.world :refer [delayed-alert
                               add-text-effect
                               add-skill
                               spawn-item
                               item-place-position
                               show-modal]]))

(defn create [_context]
  :loaded)

(defcomponent :active-skill
  (fsm/enter [[_ {:keys [eid skill]}]
              {:keys [cdq.context/elapsed-time] :as c}]
    (audio/play (:skill/start-action-sound skill))
    (when (:skill/cooldown skill)
      (swap! eid assoc-in
             [:entity/skills (:property/id skill) :skill/cooling-down?]
             (timer/create elapsed-time (:skill/cooldown skill))))
    (when (and (:skill/cost skill)
               (not (zero? (:skill/cost skill))))
      (swap! eid entity/pay-mana-cost (:skill/cost skill)))))

(defcomponent :npc-dead
  (fsm/enter [[_ {:keys [eid]}] c]
    (swap! eid assoc :entity/destroyed? true)))

(defcomponent :npc-moving
  (fsm/enter [[_ {:keys [eid movement-vector]}] c]
    (swap! eid assoc :entity/movement {:direction movement-vector
                                       :speed (or (entity/stat @eid :entity/movement-speed) 0)}))

  (fsm/exit [[_ {:keys [eid]}] c]
    (swap! eid dissoc :entity/movement)))

(defcomponent :npc-sleeping
  (fsm/exit [[_ {:keys [eid]}] c]
    (delayed-alert c
                   (:position       @eid)
                   (:entity/faction @eid)
                   0.2)
    (swap! eid add-text-effect c "[WHITE]!")))

(defcomponent :player-dead
  (fsm/enter [[_ {:keys [tx/sound
                         modal/title
                         modal/text
                         modal/button-text]}]
              c]
    (audio/play sound)
    (show-modal c {:title title
                   :text text
                   :button-text button-text
                   :on-click (fn [])})))

(defcomponent :player-idle
  (state/clicked-skillmenu-skill [[_ {:keys [eid]}] skill c]
    (let [free-skill-points (:entity/free-skill-points @eid)]
      ; TODO no else case, no visible free-skill-points
      (when (and (pos? free-skill-points)
                 (not (entity/has-skill? @eid skill)))
        (swap! eid assoc :entity/free-skill-points (dec free-skill-points))
        (add-skill c eid skill)))))

(defcomponent :player-item-on-cursor
  (fsm/enter [[_ {:keys [eid item]}] c]
    (swap! eid assoc :entity/item-on-cursor item))

  (fsm/exit [[_ {:keys [eid player-item-on-cursor/place-world-item-sound]}] c]
    ; at clicked-cell when we put it into a inventory-cell
    ; we do not want to drop it on the ground too additonally,
    ; so we dissoc it there manually. Otherwise it creates another item
    ; on the ground
    (let [entity @eid]
      (when (:entity/item-on-cursor entity)
        (audio/play place-world-item-sound)
        (swap! eid dissoc :entity/item-on-cursor)
        (spawn-item c
                    (item-place-position c entity)
                    (:entity/item-on-cursor entity))))))

(defcomponent :player-moving
  (fsm/enter [[_ {:keys [eid movement-vector]}] c]
    (swap! eid assoc :entity/movement {:direction movement-vector
                                       :speed (entity/stat @eid :entity/movement-speed)}))

  (fsm/exit [[_ {:keys [eid]}] c]
    (swap! eid dissoc :entity/movement)))
