(ns cdq.entity.state
  (:require [anvil.controls :as controls]
            [anvil.effect :as effect]
            [anvil.entity :as entity]
            [anvil.skill :as skill]
            [anvil.player :as player]
            [anvil.world.potential-field :as potential-field]
            [cdq.context :as world :refer [timer finished-ratio stopped? add-text-effect show-modal]]
            [cdq.grid :as grid]
            [cdq.inventory :as inventory]
            [clojure.component :as component :refer [defcomponent]]
            [clojure.gdx :refer [button-just-pressed? play]]
            [clojure.utils :refer [safe-merge]]
            [gdl.context :as c]
            [gdl.math.vector :as v]))

(defn- draw-skill-image [c image entity [x y] action-counter-ratio]
  (let [[width height] (:world-unit-dimensions image)
        _ (assert (= width height))
        radius (/ (float width) 2)
        y (+ (float y) (float (:half-height entity)) (float 0.15))
        center [x (+ y radius)]]
    (c/filled-circle c center radius [1 1 1 0.125])
    (c/sector c
              center
              radius
              90 ; start-angle
              (* (float action-counter-ratio) 360) ; degree
              [1 1 1 0.5])
    (c/draw-image c image [(- (float x) radius) y])))

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
      (swap! eid entity/pay-mana-cost (:skill/cost skill))))

  (component/tick [[_ {:keys [skill effect-ctx counter]}] eid c]
    (cond
     (not (effect/some-applicable? (effect/check-update-ctx c effect-ctx)
                                   (:skill/effects skill)))
     (do
      (entity/event c eid :action-done)
      ; TODO some sound ?
      )

     (stopped? c counter)
     (do
      (effect/do-all! c effect-ctx (:skill/effects skill))
      (entity/event c eid :action-done))))

  (component/render-info [[_ {:keys [skill effect-ctx counter]}] entity c]
    (let [{:keys [entity/image skill/effects]} skill]
      (draw-skill-image c
                        image
                        entity
                        (:position entity)
                        (finished-ratio c counter))
      (effect/render-info c
                          (effect/check-update-ctx c effect-ctx)
                          effects))))

(defcomponent :npc-dead
  (component/create [[_ eid] c]
    {:eid eid})

  (component/enter [[_ {:keys [eid]}] c]
    (swap! eid assoc :entity/destroyed? true)))

(defn- effect-context [c eid]
  (let [entity @eid
        target (world/nearest-enemy c entity)
        target (when (and target
                          (world/line-of-sight? c entity @target))
                 target)]
    {:effect/source eid
     :effect/target target
     :effect/target-direction (when target
                                (entity/direction entity @target))}))

(defn- npc-choose-skill [c entity ctx]
  (->> entity
       :entity/skills
       vals
       (sort-by #(or (:skill/cost %) 0))
       reverse
       (filter #(and (= :usable (skill/usable-state entity % ctx))
                     (effect/applicable-and-useful? c ctx (:skill/effects %))))
       first))

(defcomponent :npc-idle
  (component/create [[_ eid] c]
    {:eid eid})

  (component/tick [_ eid c]
    (let [effect-ctx (effect-context c eid)]
      (if-let [skill (npc-choose-skill c @eid effect-ctx)]
        (entity/event c eid :start-action [skill effect-ctx])
        (entity/event c eid :movement-direction (or (potential-field/find-direction c eid) [0 0]))))))

(defcomponent :npc-moving
  (component/create [[_ eid movement-vector] c]
    {:eid eid
     :movement-vector movement-vector
     :counter (timer c (* (entity/stat @eid :entity/reaction-time) 0.016))})

  (component/enter [[_ {:keys [eid movement-vector]}] c]
    (swap! eid assoc :entity/movement {:direction movement-vector
                                       :speed (or (entity/stat @eid :entity/movement-speed) 0)}))

  (component/exit [[_ {:keys [eid]}] c]
    (swap! eid dissoc :entity/movement))

  (component/tick [[_ {:keys [counter]}] eid c]
    (when (stopped? c counter)
      (entity/event c eid :timer-finished))))

(defcomponent :npc-sleeping
  (component/create [[_ eid] c]
    {:eid eid})

  (component/exit [[_ {:keys [eid]}] c]
    (world/delayed-alert c
                         (:position       @eid)
                         (:entity/faction @eid)
                         0.2)
    (swap! eid add-text-effect c "[WHITE]!"))

  (component/tick [_ eid c]
    (let [entity @eid
          cell (world/grid-cell c (entity/tile entity))] ; pattern!
      (when-let [distance (grid/nearest-entity-distance @cell (entity/enemy entity))]
        (when (<= distance (entity/stat entity :entity/aggro-range))
          (entity/event c eid :alert)))))

  (component/render-above [_ entity c]
    (let [[x y] (:position entity)]
      (c/draw-text c
                   {:text "zzz"
                    :x x
                    :y (+ y (:half-height entity))
                    :up? true}))))

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

(defn- world-item? [c]
  (not (c/mouse-on-actor? c)))

; It is possible to put items out of sight, losing them.
; Because line of sight checks center of entity only, not corners
; this is okay, you have thrown the item over a hill, thats possible.

(defn- placement-point [player target maxrange]
  (v/add player
         (v/scale (v/direction player target)
                  (min maxrange
                       (v/distance player target)))))

(defn- item-place-position [c entity]
  (placement-point (:position entity)
                   (c/world-mouse-position c)
                   ; so you cannot put it out of your own reach
                   (- (:entity/click-distance-tiles entity) 0.1)))

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
                    (item-place-position c entity)
                    (:entity/item-on-cursor entity)))))

  (component/manual-tick [[_ {:keys [eid]}] c]
    (when (and (button-just-pressed? c :left)
               (world-item? c))
      (entity/event c eid :drop-item)))

  (component/render-below [[_ {:keys [item]}] entity c]
    (when (world-item? c)
      (c/draw-centered c
                       (:entity/image item)
                       (item-place-position c entity))))

  (component/draw-gui-view [[_ {:keys [eid]}] c]
    (when (not (world-item? c))
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
    (swap! eid dissoc :entity/movement))

  (component/tick [[_ {:keys [movement-vector]}] eid c]
    (if-let [movement-vector (controls/movement-vector c)]
      (swap! eid assoc :entity/movement {:direction movement-vector
                                         :speed (entity/stat @eid :entity/movement-speed)})
      (entity/event c eid :no-movement-input))))

(defcomponent :stunned
  (component/create [[_ eid duration] c]
    {:eid eid
     :counter (timer c duration)})

  (component/tick [[_ {:keys [counter]}] eid c]
    (when (stopped? c counter)
      (entity/event c eid :effect-wears-off)))

  (component/render-below [_ entity c]
    (c/circle c (:position entity) 0.5 [1 1 1 0.6])))
