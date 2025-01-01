(ns cdq.component.state
  (:require [anvil.entity :as entity]
            [cdq.context :as world :refer [timer add-text-effect show-modal]]
            [clojure.component :as component :refer [defcomponent]]
            [clojure.gdx :refer [play]]))

(defcomponent :active-skill
  (component/enter [[_ {:keys [eid skill]}] c]
    (play (:skill/start-action-sound skill))
    (when (:skill/cooldown skill)
      (swap! eid assoc-in
             [:entity/skills (:property/id skill) :skill/cooling-down?]
             (timer c (:skill/cooldown skill))))
    (when (and (:skill/cost skill)
               (not (zero? (:skill/cost skill))))
      (swap! eid entity/pay-mana-cost (:skill/cost skill)))))

(defcomponent :npc-dead
  (component/enter [[_ {:keys [eid]}] c]
    (swap! eid assoc :entity/destroyed? true)))

(defcomponent :npc-moving
  (component/enter [[_ {:keys [eid movement-vector]}] c]
    (swap! eid assoc :entity/movement {:direction movement-vector
                                       :speed (or (entity/stat @eid :entity/movement-speed) 0)}))

  (component/exit [[_ {:keys [eid]}] c]
    (swap! eid dissoc :entity/movement)))

(defcomponent :npc-sleeping
  (component/exit [[_ {:keys [eid]}] c]
    (world/delayed-alert c
                         (:position       @eid)
                         (:entity/faction @eid)
                         0.2)
    (swap! eid add-text-effect c "[WHITE]!")))

(defcomponent :player-dead
  (component/enter [[_ {:keys [tx/sound
                               modal/title
                               modal/text
                               modal/button-text]}]
                    c]
    (play sound)
    (show-modal c {:title title
                   :text text
                   :button-text button-text
                   :on-click (fn [])})))

(defcomponent :player-item-on-cursor
  (component/enter [[_ {:keys [eid item]}] c]
    (swap! eid assoc :entity/item-on-cursor item))
  (component/exit [[_ {:keys [eid player-item-on-cursor/place-world-item-sound]}] c]
    ; at clicked-cell when we put it into a inventory-cell
    ; we do not want to drop it on the ground too additonally,
    ; so we dissoc it there manually. Otherwise it creates another item
    ; on the ground
    (let [entity @eid]
      (when (:entity/item-on-cursor entity)
        (play place-world-item-sound)
        (swap! eid dissoc :entity/item-on-cursor)
        (world/item c
                    (world/item-place-position c entity)
                    (:entity/item-on-cursor entity))))))

(defcomponent :player-moving
  (component/enter [[_ {:keys [eid movement-vector]}] c]
    (swap! eid assoc :entity/movement {:direction movement-vector
                                       :speed (entity/stat @eid :entity/movement-speed)}))

  (component/exit [[_ {:keys [eid]}] c]
    (swap! eid dissoc :entity/movement)))
