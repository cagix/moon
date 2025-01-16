(ns cdq.entity.state
  (:require [clojure.assets :refer [play-sound]]
            cdq.graphics
            [clojure.audio :as audio]
            [clojure.utils :refer [defcomponent]]
            [clojure.world.potential-field :as potential-field]
            [clojure.inventory :as inventory]
            [clojure.timer :as timer]
            [clojure.entity :as entity]
            [clojure.entity.state :as state]
            [clojure.grid :as grid]
            [clojure.input :as input]
            [clojure.effect-context :as effect-ctx]
            [clojure.skill :as skill]
            [cdq.stage :as stage]
            [clojure.math.vector2 :as v]
            [clojure.ui :as ui]
            [clojure.scene2d.actor :as actor]
            [clojure.world :refer [tick!
                                   nearest-enemy
                                   delayed-alert
                                   add-text-effect
                                   get-inventory
                                   pickup-item
                                   show-player-msg
                                   selected-skill
                                   player-movement-vector
                                   remove-item
                                   add-skill
                                   set-item
                                   stack-item
                                   spawn-item
                                   item-place-position
                                   world-item?
                                   show-modal
                                   send-event!
                                   line-of-sight?]]))

; this is not necessary if effect does not need target, but so far not other solution came up.
(defn- update-effect-ctx
  "Call this on effect-context if the time of using the context is not the time when context was built."
  [context {:keys [effect/source effect/target] :as effect-ctx}]
  (if (and target
           (not (:entity/destroyed? @target))
           (line-of-sight? context @source @target))
    effect-ctx
    (dissoc effect-ctx :effect/target)))

(defcomponent :active-skill
  (state/cursor [_]
    :cursors/sandclock)

  (state/pause-game? [_]
    false)

  (state/enter [[_ {:keys [eid skill]}]
                {:keys [clojure.context/elapsed-time] :as c}]
    (audio/play (:skill/start-action-sound skill))
    (when (:skill/cooldown skill)
      (swap! eid assoc-in
             [:entity/skills (:property/id skill) :skill/cooling-down?]
             (timer/create elapsed-time (:skill/cooldown skill))))
    (when (and (:skill/cost skill)
               (not (zero? (:skill/cost skill))))
      (swap! eid entity/pay-mana-cost (:skill/cost skill))))

  (tick! [[_ {:keys [skill effect-ctx counter]}]
          eid
          {:keys [clojure.context/elapsed-time] :as c}]
    (cond
     (not (effect-ctx/some-applicable? (update-effect-ctx c effect-ctx)
                                       (:skill/effects skill)))
     (do
      (send-event! c eid :action-done)
      ; TODO some sound ?
      )

     (timer/stopped? counter elapsed-time)
     (do
      (effect-ctx/do-all! c effect-ctx (:skill/effects skill))
      (send-event! c eid :action-done)))))

(defcomponent :npc-dead
  (state/enter [[_ {:keys [eid]}] c]
    (swap! eid assoc :entity/destroyed? true)))

(defn- npc-choose-skill [c entity ctx]
  (->> entity
       :entity/skills
       vals
       (sort-by #(or (:skill/cost %) 0))
       reverse
       (filter #(and (= :usable (skill/usable-state entity % ctx))
                     (effect-ctx/applicable-and-useful? c ctx (:skill/effects %))))
       first))

(defn- npc-effect-context [c eid]
  (let [entity @eid
        target (nearest-enemy c entity)
        target (when (and target
                          (line-of-sight? c entity @target))
                 target)]
    {:effect/source eid
     :effect/target target
     :effect/target-direction (when target
                                (entity/direction entity @target))}))

(defcomponent :npc-idle
  (tick! [_ eid c]
    (let [effect-ctx (npc-effect-context c eid)]
      (if-let [skill (npc-choose-skill c @eid effect-ctx)]
        (send-event! c eid :start-action [skill effect-ctx])
        (send-event! c eid :movement-direction (or (potential-field/find-direction c eid) [0 0]))))))

(defcomponent :npc-moving
  (state/enter [[_ {:keys [eid movement-vector]}] c]
    (swap! eid assoc :entity/movement {:direction movement-vector
                                       :speed (or (entity/stat @eid :entity/movement-speed) 0)}))

  (state/exit [[_ {:keys [eid]}] c]
    (swap! eid dissoc :entity/movement))

  (tick! [[_ {:keys [counter]}]
          eid
          {:keys [clojure.context/elapsed-time] :as c}]
    (when (timer/stopped? counter elapsed-time)
      (send-event! c eid :timer-finished))))

(defcomponent :npc-sleeping
  (state/exit [[_ {:keys [eid]}] c]
    (delayed-alert c
                   (:position       @eid)
                   (:entity/faction @eid)
                   0.2)
    (swap! eid add-text-effect c "[WHITE]!"))

  (tick! [_ eid {:keys [clojure.context/grid] :as c}]
    (let [entity @eid
          cell (grid (entity/tile entity))]
      (when-let [distance (grid/nearest-entity-distance @cell (entity/enemy entity))]
        (when (<= distance (entity/stat entity :entity/aggro-range))
          (send-event! c eid :alert))))))

(defcomponent :player-dead
  (state/cursor [_]
    :cursors/black-x)

  (state/pause-game? [_]
    true)

  (state/enter [[_ {:keys [tx/sound
                           modal/title
                           modal/text
                           modal/button-text]}]
                c]
    (audio/play sound)
    (show-modal c {:title title
                   :text text
                   :button-text button-text
                   :on-click (fn [])})))

(defmulti ^:private on-clicked
  (fn [eid c]
    (:type (:entity/clickable @eid))))

(defmethod on-clicked :clickable/item [eid {:keys [clojure.context/player-eid] :as c}]
  (let [item (:entity/item @eid)]
    (cond
     (actor/visible? (get-inventory c))
     (do
      (play-sound c "bfxr_takeit")
      (swap! eid assoc :entity/destroyed? true)
      (send-event! c player-eid :pickup-item item))

     (entity/can-pickup-item? @player-eid item)
     (do
      (play-sound c "bfxr_pickup")
      (swap! eid assoc :entity/destroyed? true)
      (pickup-item c player-eid item))

     :else
     (do
      (play-sound c "bfxr_denied")
      (show-player-msg c "Your Inventory is full")))))

(defmethod on-clicked :clickable/player [_ c]
  (actor/toggle-visible! (get-inventory c)))

(defn- clickable->cursor [entity too-far-away?]
  (case (:type (:entity/clickable entity))
    :clickable/item (if too-far-away?
                      :cursors/hand-before-grab-gray
                      :cursors/hand-before-grab)
    :clickable/player :cursors/bag))

(defn- clickable-entity-interaction [c player-entity clicked-eid]
  (if (< (v/distance (:position player-entity)
                     (:position @clicked-eid))
         (:entity/click-distance-tiles player-entity))
    [(clickable->cursor @clicked-eid false) (fn []
                                              (on-clicked clicked-eid c))]
    [(clickable->cursor @clicked-eid true)  (fn []
                                              (play-sound c "bfx_denied")
                                              (show-player-msg c "Too far away"))]))

(defn- inventory-cell-with-item? [{:keys [clojure.context/player-eid] :as c} actor]
  (and (actor/parent actor)
       (= "inventory-cell" (.getName (actor/parent actor)))
       (get-in (:entity/inventory @player-eid)
               (actor/user-object (actor/parent actor)))))

(defn- mouseover-actor->cursor [c]
  (let [actor (stage/mouse-on-actor? c)]
    (cond
     (inventory-cell-with-item? c actor) :cursors/hand-before-grab
     (ui/window-title-bar? actor)           :cursors/move-window
     (ui/button? actor)                     :cursors/over-button
     :else                               :cursors/default)))

(defn- player-effect-ctx [{:keys [clojure.context/mouseover-eid] :as c} eid]
  (let [target-position (or (and mouseover-eid
                                 (:position @mouseover-eid))
                            (cdq.graphics/world-mouse-position c))]
    {:effect/source eid
     :effect/target mouseover-eid
     :effect/target-position target-position
     :effect/target-direction (v/direction (:position @eid) target-position)}))

(defn- interaction-state [{:keys [clojure.context/mouseover-eid] :as c} eid]
  (let [entity @eid]
    (cond
     (stage/mouse-on-actor? c)
     [(mouseover-actor->cursor c)
      (fn [] nil)] ; handled by actors themself, they check player state

     (and mouseover-eid
          (:entity/clickable @mouseover-eid))
     (clickable-entity-interaction c entity mouseover-eid)

     :else
     (if-let [skill-id (selected-skill c)]
       (let [skill (skill-id (:entity/skills entity))
             effect-ctx (player-effect-ctx c eid)
             state (skill/usable-state entity skill effect-ctx)]
         (if (= state :usable)
           (do
            ; TODO cursor AS OF SKILL effect (SWORD !) / show already what the effect would do ? e.g. if it would kill highlight
            ; different color ?
            ; => e.g. meditation no TARGET .. etc.
            [:cursors/use-skill
             (fn []
               (send-event! c eid :start-action [skill effect-ctx]))])
           (do
            ; TODO cursor as of usable state
            ; cooldown -> sanduhr kleine
            ; not-enough-mana x mit kreis?
            ; invalid-params -> depends on params ...
            [:cursors/skill-not-usable
             (fn []
               (play-sound c "bfxr_denied")
               (show-player-msg c (case state
                                          :cooldown "Skill is still on cooldown"
                                          :not-enough-mana "Not enough mana"
                                          :invalid-params "Cannot use this here")))])))
       [:cursors/no-skill-selected
        (fn []
          (play-sound c "bfxr_denied")
          (show-player-msg c "No selected skill"))]))))

(defcomponent :player-idle
  (state/pause-game? [_]
    true)

  (state/manual-tick [[_ {:keys [eid]}] {:keys [clojure/input] :as c}]
    (if-let [movement-vector (player-movement-vector input)]
      (send-event! c eid :movement-input movement-vector)
      (let [[cursor on-click] (interaction-state c eid)]
        (cdq.graphics/set-cursor c cursor)
        (when (input/button-just-pressed? input :left)
          (on-click)))))

  (state/clicked-inventory-cell [[_ {:keys [eid player-idle/pickup-item-sound]}] cell c]
    ; TODO no else case
    (when-let [item (get-in (:entity/inventory @eid) cell)]
      (audio/play pickup-item-sound)
      (send-event! c eid :pickup-item item)
      (remove-item c eid cell)))

  (state/clicked-skillmenu-skill [[_ {:keys [eid]}] skill c]
    (let [free-skill-points (:entity/free-skill-points @eid)]
      ; TODO no else case, no visible free-skill-points
      (when (and (pos? free-skill-points)
                 (not (entity/has-skill? @eid skill)))
        (swap! eid assoc :entity/free-skill-points (dec free-skill-points))
        (add-skill c eid skill)))))

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
      (audio/play item-put-sound)
      (swap! eid dissoc :entity/item-on-cursor)
      (set-item c eid cell item-on-cursor)
      (send-event! c eid :dropped-item))

     ; STACK ITEMS
     (and item-in-cell
          (inventory/stackable? item-in-cell item-on-cursor))
     (do
      (audio/play item-put-sound)
      (swap! eid dissoc :entity/item-on-cursor)
      (stack-item c eid cell item-on-cursor)
      (send-event! c eid :dropped-item))

     ; SWAP ITEMS
     (and item-in-cell
          (inventory/valid-slot? cell item-on-cursor))
     (do
      (audio/play item-put-sound)
      ; need to dissoc and drop otherwise state enter does not trigger picking it up again
      ; TODO? coud handle pickup-item from item-on-cursor state also
      (swap! eid dissoc :entity/item-on-cursor)
      (remove-item c eid cell)
      (set-item c eid cell item-on-cursor)
      (send-event! c eid :dropped-item)
      (send-event! c eid :pickup-item item-in-cell)))))

(defcomponent :player-item-on-cursor
  (state/cursor [_]
    :cursors/hand-grab)

  (state/pause-game? [_]
    true)

  (state/enter [[_ {:keys [eid item]}] c]
    (swap! eid assoc :entity/item-on-cursor item))

  (state/exit [[_ {:keys [eid player-item-on-cursor/place-world-item-sound]}] c]
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
                    (:entity/item-on-cursor entity)))))

  (state/manual-tick [[_ {:keys [eid]}] {:keys [clojure/input] :as c}]
    (when (and (input/button-just-pressed? input :left)
               (world-item? c))
      (send-event! c eid :drop-item)))

  (state/clicked-inventory-cell [[_ {:keys [eid] :as data}] cell c]
    (clicked-cell data eid cell c)))

(defcomponent :player-moving
  (state/cursor [_]
    :cursors/walking)

  (state/pause-game? [_]
    false)

  (state/enter [[_ {:keys [eid movement-vector]}] c]
    (swap! eid assoc :entity/movement {:direction movement-vector
                                       :speed (entity/stat @eid :entity/movement-speed)}))

  (state/exit [[_ {:keys [eid]}] c]
    (swap! eid dissoc :entity/movement))

  (tick! [[_ {:keys [movement-vector]}] eid {:keys [clojure/input] :as c}]
    (if-let [movement-vector (player-movement-vector input)]
      (swap! eid assoc :entity/movement {:direction movement-vector
                                         :speed (entity/stat @eid :entity/movement-speed)})
      (send-event! c eid :no-movement-input))))

(defcomponent :stunned
  (state/cursor [_]
    :cursors/denied)

  (state/pause-game? [_]
    false)

  (tick! [[_ {:keys [counter]}]
          eid
          {:keys [clojure.context/elapsed-time] :as c}]
    (when (timer/stopped? counter elapsed-time)
      (send-event! c eid :effect-wears-off))))
