(ns cdq.entity.state
  (:require [anvil.controls :as controls]
            [anvil.effect :as effect]
            [anvil.entity :as entity]
            [anvil.player :as player]
            [cdq.context :as world :refer [timer finished-ratio stopped? add-text-effect show-modal]]
            [cdq.grid :as grid]
            [cdq.inventory :as inventory]
            [clojure.component :as component :refer [defcomponent]]
            [clojure.gdx :refer [button-just-pressed? play]]
            [clojure.utils :refer [safe-merge]]
            [gdl.context :as c]))

(defn- apply-action-speed-modifier [entity skill action-time]
  (/ action-time
     (or (entity/stat entity (:skill/action-time-modifier-key skill))
         1)))

(defcomponent :active-skill
  (component/create [[_ eid [skill effect-ctx]] c]
    {:eid eid
     :skill skill
     :effect-ctx effect-ctx
     :counter (->> skill
                   :skill/action-time
                   (apply-action-speed-modifier @eid skill)
                   (timer c))})

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
  (component/create [[_ eid] c]
    {:eid eid})

  (component/enter [[_ {:keys [eid]}] c]
    (swap! eid assoc :entity/destroyed? true)))

(defcomponent :npc-idle
  (component/create [[_ eid] c]
    {:eid eid}))

(defcomponent :npc-moving
  (component/create [[_ eid movement-vector] c]
    {:eid eid
     :movement-vector movement-vector
     :counter (timer c (* (entity/stat @eid :entity/reaction-time) 0.016))})

  (component/enter [[_ {:keys [eid movement-vector]}] c]
    (swap! eid assoc :entity/movement {:direction movement-vector
                                       :speed (or (entity/stat @eid :entity/movement-speed) 0)}))

  (component/exit [[_ {:keys [eid]}] c]
    (swap! eid dissoc :entity/movement)))

(defcomponent :npc-sleeping
  (component/create [[_ eid] c]
    {:eid eid})

  (component/exit [[_ {:keys [eid]}] c]
    (world/delayed-alert c
                         (:position       @eid)
                         (:entity/faction @eid)
                         0.2)
    (swap! eid add-text-effect c "[WHITE]!")))

(defcomponent :player-dead
  (component/create [[k] c]
    (c/build c :player-dead/component.enter))

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

(defcomponent :player-idle
  (component/create [[_ eid] c]
    (safe-merge (c/build c :player-idle/clicked-inventory-cell)
                {:eid eid}))

  (component/manual-tick [[_ {:keys [eid]}] c]
    (if-let [movement-vector (controls/movement-vector c)]
      (entity/event c eid :movement-input movement-vector)
      (let [[cursor on-click] (player/interaction-state c eid)]
        (c/set-cursor c cursor)
        (when (button-just-pressed? c :left)
          (on-click)))))

  (component/clicked-inventory-cell [[_ {:keys [eid player-idle/pickup-item-sound]}] cell c]
    ; TODO no else case
    (when-let [item (get-in (:entity/inventory @eid) cell)]
      (play pickup-item-sound)
      (entity/event c eid :pickup-item item)
      (entity/remove-item c eid cell))))

(defn- clicked-cell [{:keys [player-item-on-cursor/item-put-sound]} eid cell c]
  (let [entity @eid
        inventory (:entity/inventory entity)
        item-in-cell (get-in inventory cell)
        item-on-cursor (:entity/item-on-cursor entity)]
    (cond
     ; PUT ITEM IN EMPTY CELL
     (and (not item-in-cell)
          (inventory/valid-slot? cell item-on-cursor))
     (do
      (play item-put-sound)
      (swap! eid dissoc :entity/item-on-cursor)
      (entity/set-item c eid cell item-on-cursor)
      (entity/event c eid :dropped-item))

     ; STACK ITEMS
     (and item-in-cell
          (inventory/stackable? item-in-cell item-on-cursor))
     (do
      (play item-put-sound)
      (swap! eid dissoc :entity/item-on-cursor)
      (entity/stack-item c eid cell item-on-cursor)
      (entity/event c eid :dropped-item))

     ; SWAP ITEMS
     (and item-in-cell
          (inventory/valid-slot? cell item-on-cursor))
     (do
      (play item-put-sound)
      ; need to dissoc and drop otherwise state enter does not trigger picking it up again
      ; TODO? coud handle pickup-item from item-on-cursor state also
      (swap! eid dissoc :entity/item-on-cursor)
      (entity/remove-item c eid cell)
      (entity/set-item c eid cell item-on-cursor)
      (entity/event c eid :dropped-item)
      (entity/event c eid :pickup-item item-in-cell)))))

(defcomponent :player-item-on-cursor
  (component/create [[_ eid item] c]
    (safe-merge (c/build c :player-item-on-cursor/component)
                {:eid eid
                 :item item}))

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
                    (:entity/item-on-cursor entity)))))

  (component/manual-tick [[_ {:keys [eid]}] c]
    (when (and (button-just-pressed? c :left)
               (world/world-item? c))
      (entity/event c eid :drop-item)))

  (component/draw-gui-view [[_ {:keys [eid]}] c]
    (when (not (world/world-item? c))
      (c/draw-centered c
                       (:entity/image (:entity/item-on-cursor @eid))
                       (c/mouse-position c))))

  (component/clicked-inventory-cell [[_ {:keys [eid] :as data}] cell c]
    (clicked-cell data eid cell c)))

(defcomponent :player-moving
  (component/create [[_ eid movement-vector] c]
    {:eid eid
     :movement-vector movement-vector})

  (component/enter [[_ {:keys [eid movement-vector]}] c]
    (swap! eid assoc :entity/movement {:direction movement-vector
                                       :speed (entity/stat @eid :entity/movement-speed)}))

  (component/exit [[_ {:keys [eid]}] c]
    (swap! eid dissoc :entity/movement)))

(defcomponent :stunned
  (component/create [[_ eid duration] c]
    {:eid eid
     :counter (timer c duration)}))
