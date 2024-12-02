(ns forge.entity.states
  (:require [forge.controls :as controls]
            [forge.entity :refer [->v tick render-below render-above render-info]]
            [forge.entity.components :as entity]
            [forge.entity.inventory :as inventory]
            [forge.entity.state :as state]
            [forge.follow-ai :as follow-ai]
            [forge.item :refer [valid-slot? stackable?]]
            [forge.ui :as ui]
            [forge.ui.action-bar :as action-bar]
            [forge.ui.inventory :as widgets.inventory]
            [forge.ui.player-message :as player-message]
            [forge.ui.modal :as modal]
            [forge.world :as world :refer [timer stopped? line-of-sight? finished-ratio player-eid mouseover-eid]]))

(comment
 (def ^:private entity-state
   (merge-with concat
               entity
               {:optional [#'state/enter
                           #'state/exit
                           #'state/cursor
                           #'state/pause-game?
                           #'state/manual-tick
                           #'state/clicked-inventory-cell
                           #'state/clicked-skillmenu-skill
                           #'state/draw-gui-view]})))

(defmethod ->v :npc-dead [[_ eid]]
  {:eid eid})

(defmethod state/enter :npc-dead [[_ {:keys [eid]}]]
  (swap! eid assoc :entity/destroyed? true))

(defn- nearest-enemy [entity]
  (world/nearest-entity @(world/cell (entity/tile entity))
                        (entity/enemy entity)))

(defn- npc-effect-ctx [eid]
  (let [entity @eid
        target (nearest-enemy entity)
        target (when (and target (line-of-sight? entity @target))
                 target)]
    {:effect/source eid
     :effect/target target
     :effect/target-direction (when target (entity/direction entity @target))}))

(comment
 (let [eid (world/ids->eids 76)
       effect-ctx (npc-effect-ctx eid)]
   (npc-choose-skill effect-ctx @eid))
 )

(defn- npc-choose-skill [entity ctx]
  (->> entity
       :entity/skills
       vals
       (sort-by #(or (:skill/cost %) 0))
       reverse
       (filter #(and (= :usable (entity/skill-usable-state entity % ctx))
                     (effects-useful? ctx (:skill/effects %))))
       first))

(defmethods :npc-idle
  (->v [[_ eid]]
    {:eid eid})

  (tick [_ eid]
    (let [effect-ctx (npc-effect-ctx eid)]
      (if-let [skill (npc-choose-skill @eid effect-ctx)]
        (entity/event eid :start-action [skill effect-ctx])
        (entity/event eid :movement-direction (or (follow-ai/direction-vector eid) [0 0]))))))

; npc moving is basically a performance optimization so npcs do not have to check
; usable skills every frame
; also prevents fast twitching around changing directions every frame
(defmethods :npc-moving
  (->v [[_ eid movement-vector]]
    {:eid eid
     :movement-vector movement-vector
     :counter (timer (* (entity/stat @eid :entity/reaction-time) 0.016))})

  (state/enter [{:keys [eid movement-vector]}]
    (swap! eid assoc :entity/movement {:direction movement-vector
                                       :speed (or (entity/stat @eid :entity/movement-speed) 0)}))

  (state/exit [{:keys [eid]}]
    (swap! eid dissoc :entity/movement))

  (tick [{:keys [counter]} eid]
    (when (stopped? counter)
      (entity/event eid :timer-finished))))

(defmethods :npc-sleeping
  (->v [[_ eid]]
    {:eid eid})

  (state/exit [[_ {:keys [eid]}]]
    (world/shout (:position @eid) (:entity/faction @eid) 0.2)
    (swap! eid entity/add-text-effect "[WHITE]!"))

  (tick [_ eid]
    (let [entity @eid
          cell (world/cell (entity/tile entity))] ; pattern!
      (when-let [distance (world/nearest-entity-distance @cell (entity/enemy entity))]
        (when (<= distance (entity/stat entity :entity/aggro-range))
          (entity/event eid :alert)))))

  (render-above [_ entity]
    (let [[x y] (:position entity)]
      (draw-text {:text "zzz"
                  :x x
                  :y (+ y (:half-height entity))
                  :up? true}))))

(defmethods :player-dead
  (state/cursor [_]
    :cursors/black-x)

  (state/pause-game? [_]
    true)

  (state/enter [_]
    (play-sound "bfxr_playerdeath")
    (modal/show {:title "YOU DIED"
                 :text "\nGood luck next time"
                 :button-text ":("
                 :on-click #(change-screen :screens/main-menu)})))

(defmethods :stunned
  (->v [[_ eid duration]]
    {:eid eid
     :counter (timer duration)})

  (state/cursor [_]
    :cursors/denied)

  (state/pause-game? [_]
    false)

  (tick [[_ {:keys [counter]}] eid]
    (when (stopped? counter)
      (entity/event eid :effect-wears-off)))

  (render-below [_ entity]
    (draw-circle (:position entity) 0.5 [1 1 1 0.6])))

(defmethods :player-moving
  (->v [[_ eid movement-vector]]
    {:eid eid
     :movement-vector movement-vector})

  (state/cursor [_]
    :cursors/walking)

  (state/pause-game? [_]
    false)

  (state/enter [[_ {:keys [eid movement-vector]}]]
    (swap! eid assoc :entity/movement {:direction movement-vector
                                       :speed (entity/stat @eid :entity/movement-speed)}))

  (state/exit [[_ {:keys [eid]}]]
    (swap! eid dissoc :entity/movement))

  (tick [[_ {:keys [movement-vector]}] eid]
    (if-let [movement-vector (controls/movement-vector)]
      (swap! eid assoc :entity/movement {:direction movement-vector
                                         :speed (entity/stat @eid :entity/movement-speed)})
      (entity/event eid :no-movement-input))))

(defn- apply-action-speed-modifier [entity skill action-time]
  (/ action-time
     (or (entity/stat entity (:skill/action-time-modifier-key skill))
         1)))

; this is not necessary if effect does not need target, but so far not other solution came up.
(defn- check-update-ctx
  "Call this on effect-context if the time of using the context is not the time when context was built."
  [{:keys [effect/source effect/target] :as ctx}]
  (if (and target
           (not (:entity/destroyed? @target))
           (line-of-sight? @source @target))
    ctx
    (dissoc ctx :effect/target)))

(defn- draw-skill-image [image entity [x y] action-counter-ratio]
  (let [[width height] (:world-unit-dimensions image)
        _ (assert (= width height))
        radius (/ (float width) 2)
        y (+ (float y) (float (:half-height entity)) (float 0.15))
        center [x (+ y radius)]]
    (draw-filled-circle center radius [1 1 1 0.125])
    (draw-sector center radius
                 90 ; start-angle
                 (* (float action-counter-ratio) 360) ; degree
                 [1 1 1 0.5])
    (draw-image image [(- (float x) radius) y])))

(defmethods :active-skill
  (->v [[_ eid [skill effect-ctx]]]
    {:eid eid
     :skill skill
     :effect-ctx effect-ctx
     :counter (->> skill
                   :skill/action-time
                   (apply-action-speed-modifier @eid skill)
                   timer)})

  (state/cursor [_]
    :cursors/sandclock)

  (state/pause-game? [_]
    false)

  (state/enter [[_ {:keys [eid skill]}]]
    (play-sound (:skill/start-action-sound skill))
    (when (:skill/cooldown skill)
      (swap! eid assoc-in
             [:entity/skills (:property/id skill) :skill/cooling-down?]
             (timer (:skill/cooldown skill))))
    (when (and (:skill/cost skill)
               (not (zero? (:skill/cost skill))))
      (swap! eid entity/pay-mana-cost (:skill/cost skill))))

  (tick [[_ {:keys [skill effect-ctx counter]}] eid]
    (cond
     (not (effects-applicable? (check-update-ctx effect-ctx)
                               (:skill/effects skill)))
     (do
      (entity/event eid :action-done)
      ; TODO some sound ?
      )

     (stopped? counter)
     (do
      (effects-do! effect-ctx (:skill/effects skill))
      (entity/event eid :action-done))))

  (render-info [[_ {:keys [skill effect-ctx counter]}] entity]
    (let [{:keys [entity/image skill/effects]} skill]
      (draw-skill-image image entity (:position entity) (finished-ratio counter))
      (effects-render (check-update-ctx effect-ctx) effects))))

(defn- denied [text]
  (play-sound "bfxr_denied")
  (player-message/show text))

(defmulti ^:private on-clicked
  (fn [eid]
    (:type (:entity/clickable @eid))))

(defmethod on-clicked :clickable/item [eid]
  (let [item (:entity/item @eid)]
    (cond
     (visible? (widgets.inventory/window))
     (do
      (play-sound "bfxr_takeit")
      (swap! eid assoc :entity/destroyed? true)
      (entity/event player-eid :pickup-item item))

     (inventory/can-pickup-item? @player-eid item)
     (do
      (play-sound "bfxr_pickup")
      (swap! eid assoc :entity/destroyed? true)
      (inventory/pickup-item player-eid item))

     :else
     (do
      (play-sound "bfxr_denied")
      (player-message/show "Your Inventory is full")))))

(defmethod on-clicked :clickable/player [_]
  (ui/toggle-visible! (widgets.inventory/window)))

(defn- clickable->cursor [entity too-far-away?]
  (case (:type (:entity/clickable entity))
    :clickable/item (if too-far-away?
                      :cursors/hand-before-grab-gray
                      :cursors/hand-before-grab)
    :clickable/player :cursors/bag))

(defn- clickable-entity-interaction [player-entity clicked-eid]
  (if (< (v-distance (:position player-entity)
                     (:position @clicked-eid))
         (:entity/click-distance-tiles player-entity))
    [(clickable->cursor @clicked-eid false) (fn [] (on-clicked clicked-eid))]
    [(clickable->cursor @clicked-eid true)  (fn [] (denied "Too far away"))]))

(defn- inventory-cell-with-item? [^com.badlogic.gdx.scenes.scene2d.Actor actor]
  (and (.getParent actor)
       (= "inventory-cell" (.getName (.getParent actor)))
       (get-in (:entity/inventory @player-eid)
               (.getUserObject (.getParent actor)))))

(defn- mouseover-actor->cursor []
  (let [actor (mouse-on-actor?)]
    (cond
     (inventory-cell-with-item? actor) :cursors/hand-before-grab
     (ui/window-title-bar? actor)      :cursors/move-window
     (ui/button? actor)                :cursors/over-button
     :else                             :cursors/default)))

(defn- player-effect-ctx [eid]
  (let [target-position (or (and mouseover-eid (:position @mouseover-eid))
                            (world-mouse-position))]
    {:effect/source eid
     :effect/target mouseover-eid
     :effect/target-position target-position
     :effect/target-direction (v-direction (:position @eid) target-position)}))

(defn- interaction-state [eid]
  (let [entity @eid]
    (cond
     (mouse-on-actor?)
     [(mouseover-actor->cursor) (fn [] nil)] ; handled by actors themself, they check player state

     (and mouseover-eid (:entity/clickable @mouseover-eid))
     (clickable-entity-interaction entity mouseover-eid)

     :else
     (if-let [skill-id (action-bar/selected-skill)]
       (let [skill (skill-id (:entity/skills entity))
             effect-ctx (player-effect-ctx eid)
             state (entity/skill-usable-state entity skill effect-ctx)]
         (if (= state :usable)
           (do
            ; TODO cursor AS OF SKILL effect (SWORD !) / show already what the effect would do ? e.g. if it would kill highlight
            ; different color ?
            ; => e.g. meditation no TARGET .. etc.
            [:cursors/use-skill
             (fn [] (entity/event eid :start-action [skill effect-ctx]))])
           (do
            ; TODO cursor as of usable state
            ; cooldown -> sanduhr kleine
            ; not-enough-mana x mit kreis?
            ; invalid-params -> depends on params ...
            [:cursors/skill-not-usable
             (fn []
               (denied (case state
                         :cooldown "Skill is still on cooldown"
                         :not-enough-mana "Not enough mana"
                         :invalid-params "Cannot use this here")))])))
       [:cursors/no-skill-selected
        (fn [] (denied "No selected skill"))]))))

(defmethods :player-idle
  (->v [[_ eid]]
    {:eid eid})

  (state/pause-game? [_]
    true)

  (state/manual-tick [[_ {:keys [eid]}]]
    (if-let [movement-vector (controls/movement-vector)]
      (entity/event eid :movement-input movement-vector)
      (let [[cursor on-click] (interaction-state eid)]
        (set-cursor cursor)
        (when (button-just-pressed? :left)
          (on-click)))))

  (state/clicked-inventory-cell [[_ {:keys [eid]}] cell]
    ; TODO no else case
    (when-let [item (get-in (:entity/inventory @eid) cell)]
      (play-sound "bfxr_takeit")
      (entity/event eid :pickup-item item)
      (inventory/remove-item eid cell)))

  (state/clicked-skillmenu-skill [[_ {:keys [eid]}] skill]
    (let [free-skill-points (:entity/free-skill-points @eid)]
      ; TODO no else case, no visible free-skill-points
      (when (and (pos? free-skill-points)
                 (not (entity/has-skill? @eid skill)))
        (swap! eid assoc :entity/free-skill-points (dec free-skill-points))
        (swap! eid entity/add-skill skill)))))

(defn- clicked-cell [eid cell]
  (let [entity @eid
        inventory (:entity/inventory entity)
        item-in-cell (get-in inventory cell)
        item-on-cursor (:entity/item-on-cursor entity)]
    (cond
     ; PUT ITEM IN EMPTY CELL
     (and (not item-in-cell)
          (valid-slot? cell item-on-cursor))
     (do
      (play-sound "bfxr_itemput")
      (swap! eid dissoc :entity/item-on-cursor)
      (inventory/set-item eid cell item-on-cursor)
      (entity/event eid :dropped-item))

     ; STACK ITEMS
     (and item-in-cell
          (stackable? item-in-cell item-on-cursor))
     (do
      (play-sound "bfxr_itemput")
      (swap! eid dissoc :entity/item-on-cursor)
      (inventory/stack-item eid cell item-on-cursor)
      (entity/event eid :dropped-item))

     ; SWAP ITEMS
     (and item-in-cell
          (valid-slot? cell item-on-cursor))
     (do
      (play-sound "bfxr_itemput")
      ; need to dissoc and drop otherwise state enter does not trigger picking it up again
      ; TODO? coud handle pickup-item from item-on-cursor state also
      (swap! eid dissoc :entity/item-on-cursor)
      (inventory/remove-item eid cell)
      (inventory/set-item eid cell item-on-cursor)
      (entity/event eid :dropped-item)
      (entity/event eid :pickup-item item-in-cell)))))

; It is possible to put items out of sight, losing them.
; Because line of sight checks center of entity only, not corners
; this is okay, you have thrown the item over a hill, thats possible.

(defn- placement-point [player target maxrange]
  (v-add player
         (v-scale (v-direction player target)
                  (min maxrange
                       (v-distance player target)))))

(defn- item-place-position [entity]
  (placement-point (:position entity)
                   (world-mouse-position)
                   ; so you cannot put it out of your own reach
                   (- (:entity/click-distance-tiles entity) 0.1)))

(defn- world-item? []
  (not (mouse-on-actor?)))

(defmethods :player-item-on-cursor
  (->v [[_ eid item]]
    {:eid eid
     :item item})

  (state/pause-game? [_]
    true)

  (state/manual-tick [[_ {:keys [eid]}]]
    (when (and (button-just-pressed? :left)
               (world-item?))
      (entity/event eid :drop-item)))

  (state/clicked-inventory-cell [[_ {:keys [eid]}] cell]
    (clicked-cell eid cell))

  (state/cursor [_]
    :cursors/hand-grab)

  (state/enter [[_ {:keys [eid item]}]]
    (swap! eid assoc :entity/item-on-cursor item))

  (state/exit [[_ {:keys [eid]}]]
    ; at clicked-cell when we put it into a inventory-cell
    ; we do not want to drop it on the ground too additonally,
    ; so we dissoc it there manually. Otherwise it creates another item
    ; on the ground
    (let [entity @eid]
      (when (:entity/item-on-cursor entity)
        (play-sound "bfxr_itemputground")
        (swap! eid dissoc :entity/item-on-cursor)
        (world/item (item-place-position entity) (:entity/item-on-cursor entity)))))

  (state/draw-gui-view [[_ {:keys [eid]}]]
    (let [entity @eid]
      (when (and (= :player-item-on-cursor (entity/state-k entity))
                 (not (world-item?)))
        (draw-centered (:entity/image (:entity/item-on-cursor entity))
                       (gui-mouse-position)))))

  (render-below [[_ {:keys [item]}] entity]
    (when (world-item?)
      (draw-centered (:entity/image item)
                     (item-place-position entity)))))
