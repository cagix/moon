(ns cdq.impl.entity
  (:require [cdq.animation :as animation]
            [cdq.ctx :as ctx]
            [cdq.db :as db]
            [cdq.effect :as effect]
            [cdq.entity :as entity]
            [cdq.draw :as draw]
            [cdq.input :as input]
            [cdq.inventory :as inventory]
            [cdq.stage :as stage]
            [cdq.state :as state]
            [cdq.timer :as timer]
            [cdq.utils :as utils :refer [defcomponent find-first]]
            [cdq.val-max :as val-max]
            [cdq.vector2 :as v]
            [cdq.world :as world]
            [cdq.world.grid :as grid]
            [cdq.world.grid.cell :as cell]
            [gdl.graphics.viewport :as viewport]
            [gdl.input]
            [malli.core :as m]
            [reduce-fsm :as fsm])
  (:import (com.badlogic.gdx.scenes.scene2d Actor)))

(defn- not-enough-mana? [entity {:keys [skill/cost]}]
  (and cost (> cost (entity/mana-val entity))))

(defn- skill-usable-state
  [entity {:keys [skill/cooling-down? skill/effects] :as skill} effect-ctx]
  (cond
   cooling-down?
   :cooldown

   (not-enough-mana? entity skill)
   :not-enough-mana

   (not (effect/some-applicable? effect-ctx effects))
   :invalid-params

   :else
   :usable))

(defmethod entity/create! :entity/player? [_ eid]
  (utils/bind-root #'ctx/player-eid eid))

(defmulti ^:private on-clicked
  (fn [eid]
    (:type (:entity/clickable @eid))))

(defmethod on-clicked :clickable/item [eid]
  (let [item (:entity/item @eid)]
    (cond
     (-> ctx/stage :windows :inventory-window Actor/.isVisible)
     [[:tx/sound "bfxr_takeit"]
      [:tx/mark-destroyed eid]
      [:tx/event ctx/player-eid :pickup-item item]]

     (inventory/can-pickup-item? (:entity/inventory @ctx/player-eid) item)
     [[:tx/sound "bfxr_pickup"]
      [:tx/mark-destroyed eid]
      [:tx/pickup-item ctx/player-eid item]]

     :else
     [[:tx/sound "bfxr_denied"]
      [:tx/show-message "Your Inventory is full"]])))

(defmethod on-clicked :clickable/player [_]
  [[:tx/toggle-inventory-visible]]) ; TODO every 'transaction' should have a sound or effect with it?

(defn- clickable->cursor [entity too-far-away?]
  (case (:type (:entity/clickable entity))
    :clickable/item (if too-far-away?
                      :cursors/hand-before-grab-gray
                      :cursors/hand-before-grab)
    :clickable/player :cursors/bag))

(defn- clickable-entity-interaction [player-entity clicked-eid]
  (if (< (v/distance (:position player-entity)
                     (:position @clicked-eid))
         (:entity/click-distance-tiles player-entity))
    [(clickable->cursor @clicked-eid false) (on-clicked clicked-eid)]
    [(clickable->cursor @clicked-eid true)  [[:tx/sound "bfxr_denied"]
                                             [:tx/show-message "Too far away"]]]))

(defn- mouseover-actor->cursor [actor]
  (cond
   (cdq.ui.inventory/cell-with-item? actor) :cursors/hand-before-grab
   (ui/window-title-bar? actor) :cursors/move-window
   (ui/button? actor) :cursors/over-button
   :else :cursors/default))

(defn- player-effect-ctx [eid]
  (let [target-position (or (and ctx/mouseover-eid
                                 (:position @ctx/mouseover-eid))
                            (viewport/mouse-position ctx/world-viewport))]
    {:effect/source eid
     :effect/target ctx/mouseover-eid
     :effect/target-position target-position
     :effect/target-direction (v/direction (:position @eid) target-position)}))

(defn- interaction-state [eid]
  (let [entity @eid]
    (cond
     (stage/mouse-on-actor? ctx/stage)
     [(mouseover-actor->cursor (stage/mouse-on-actor? ctx/stage))
      nil] ; handled by actors themself, they check player state

     (and ctx/mouseover-eid
          (:entity/clickable @ctx/mouseover-eid))
     (clickable-entity-interaction entity ctx/mouseover-eid)

     :else
     (if-let [skill-id (stage/selected-skill ctx/stage)]
       (let [skill (skill-id (:entity/skills entity))
             effect-ctx (player-effect-ctx eid)
             state (skill-usable-state entity skill effect-ctx)]
         (if (= state :usable)
           ; TODO cursor AS OF SKILL effect (SWORD !) / show already what the effect would do ? e.g. if it would kill highlight
           ; different color ?
           ; => e.g. meditation no TARGET .. etc.
           [:cursors/use-skill
            [[:tx/event eid :start-action [skill effect-ctx]]]]
           ; TODO cursor as of usable state
           ; cooldown -> sanduhr kleine
           ; not-enough-mana x mit kreis?
           ; invalid-params -> depends on params ...
           [:cursors/skill-not-usable
            [[:tx/sound "bfxr_denied"]
             [:tx/show-message (case state
                                 :cooldown "Skill is still on cooldown"
                                 :not-enough-mana "Not enough mana"
                                 :invalid-params "Cannot use this here")]]]))
       [:cursors/no-skill-selected
        [[:tx/sound "bfxr_denied"]
         [:tx/show-message "No selected skill"]]]))))

(defcomponent :player-idle
  (entity/create [[_ eid]]
    {:eid eid})

  (state/pause-game? [_] true)

  (state/manual-tick [[_ {:keys [eid]}]]
    (if-let [movement-vector (input/player-movement-vector)]
      [[:tx/event eid :movement-input movement-vector]]
      (let [[cursor on-click] (interaction-state eid)]
        (cons [:tx/set-cursor cursor]
              (when (gdl.input/button-just-pressed? :left)
                on-click)))))

  (state/clicked-inventory-cell [[_ {:keys [eid]}] cell]
    ; TODO no else case
    (when-let [item (get-in (:entity/inventory @eid) cell)]
      [[:tx/sound "bfxr_takeit"]
       [:tx/event eid :pickup-item item]
       [:tx/remove-item eid cell]])))

(defn- clicked-cell [eid cell]
  (let [entity @eid
        inventory (:entity/inventory entity)
        item-in-cell (get-in inventory cell)
        item-on-cursor (:entity/item-on-cursor entity)]
    (cond
     ; PUT ITEM IN EMPTY CELL
     (and (not item-in-cell)
          (inventory/valid-slot? cell item-on-cursor))
     [[:tx/sound "bfxr_itemput"]
      [:tx/dissoc eid :entity/item-on-cursor]
      [:tx/set-item eid cell item-on-cursor]
      [:tx/event eid :dropped-item]]

     ; STACK ITEMS
     (and item-in-cell
          (inventory/stackable? item-in-cell item-on-cursor))
     [[:tx/sound "bfxr_itemput"]
      [:tx/dissoc eid :entity/item-on-cursor]
      [:tx/stack-item eid cell item-on-cursor]
      [:tx/event eid :dropped-item]]

     ; SWAP ITEMS
     (and item-in-cell
          (inventory/valid-slot? cell item-on-cursor))
     [[:tx/sound "bfxr_itemput"]
      ; need to dissoc and drop otherwise state enter does not trigger picking it up again
      ; TODO? coud handle pickup-item from item-on-cursor state also
      [:tx/dissoc eid :entity/item-on-cursor]
      [:tx/remove-item eid cell]
      [:tx/set-item eid cell item-on-cursor]
      [:tx/event eid :dropped-item]
      [:tx/event eid :pickup-item item-in-cell]])))

(defn- world-item? []
  (not (stage/mouse-on-actor? ctx/stage)))

; It is possible to put items out of sight, losing them.
; Because line of sight checks center of entity only, not corners
; this is okay, you have thrown the item over a hill, thats possible.
(defn- placement-point [player target maxrange]
  (v/add player
         (v/scale (v/direction player target)
                  (min maxrange
                       (v/distance player target)))))

(defn- item-place-position [entity]
  (placement-point (:position entity)
                   (viewport/mouse-position ctx/world-viewport)
                   ; so you cannot put it out of your own reach
                   (- (:entity/click-distance-tiles entity) 0.1)))

(defcomponent :player-item-on-cursor
  (entity/create [[_ eid item]]
    {:eid eid
     :item item})

  (entity/render-below! [[_ {:keys [item]}] entity]
    (when (world-item?)
      (draw/centered (:entity/image item)
                     (item-place-position entity))))

  (state/cursor [_] :cursors/hand-grab)

  (state/pause-game? [_] true)

  (state/enter! [[_ {:keys [eid item]}]]
    [[:tx/assoc eid :entity/item-on-cursor item]])

  (state/exit! [[_ {:keys [eid]}]]
    ; at clicked-cell when we put it into a inventory-cell
    ; we do not want to drop it on the ground too additonally,
    ; so we dissoc it there manually. Otherwise it creates another item
    ; on the ground
    (let [entity @eid]
      (when (:entity/item-on-cursor entity)
        [[:tx/sound "bfxr_itemputground"]
         [:tx/dissoc eid :entity/item-on-cursor]
         [:tx/spawn-item (item-place-position entity) (:entity/item-on-cursor entity)]])))

  (state/manual-tick [[_ {:keys [eid]}]]
    (when (and (gdl.input/button-just-pressed? :left)
               (world-item?))
      [[:tx/event eid :drop-item]]))

  (state/clicked-inventory-cell [[_ {:keys [eid]}] cell]
    (clicked-cell eid cell))

  (state/draw-gui-view [[_ {:keys [eid]}]]
    (when (not (world-item?))
      (draw/centered (:entity/image (:entity/item-on-cursor @eid))
                     (viewport/mouse-position ctx/ui-viewport)))))

(defcomponent :entity/delete-after-duration
  (entity/create [[_ duration]]
    (timer/create ctx/elapsed-time duration))

  (entity/tick! [[_ counter] eid]
    (when (timer/stopped? ctx/elapsed-time counter)
      [[:tx/mark-destroyed eid]])))

(defmethod entity/create :entity/hp [[_ v]]
  [v v])

(defmethod entity/create :entity/mana [[_ v]]
  [v v])

(defmethod entity/create :entity/projectile-collision [[_ v]]
  (assoc v :already-hit-bodies #{}))

(defn- apply-action-speed-modifier [entity skill action-time]
  (/ action-time
     (or (entity/stat entity (:skill/action-time-modifier-key skill))
         1)))

(defmethod entity/create :active-skill [[_ eid [skill effect-ctx]]]
  {:eid eid
   :skill skill
   :effect-ctx effect-ctx
   :counter (->> skill
                 :skill/action-time
                 (apply-action-speed-modifier @eid skill)
                 (timer/create ctx/elapsed-time))})

(defmethod entity/create :npc-dead [[_ eid]]
  {:eid eid})

(defmethod entity/create :npc-idle [[_ eid]]
  {:eid eid})

(defmethod entity/create :npc-moving [[_ eid movement-vector]]
  {:eid eid
   :movement-vector movement-vector
   :counter (timer/create ctx/elapsed-time (* (entity/stat @eid :entity/reaction-time) 0.016))})

(defmethod entity/create :npc-sleeping [[_ eid]]
  {:eid eid})

(defmethod entity/create :player-moving [[_ eid movement-vector]]
  {:eid eid
   :movement-vector movement-vector})

(defmethod entity/create :stunned [[_ eid duration]]
  {:eid eid
   :counter (timer/create ctx/elapsed-time duration)})

(defmethod entity/create! :entity/inventory [[k items] eid]
  (cons [:tx/assoc eid k inventory/empty-inventory]
        (for [item items]
          [:tx/pickup-item eid item])))

(defmethod entity/create! :entity/skills [[k skills] eid]
  (cons [:tx/assoc eid k nil]
        (for [skill skills]
          [:tx/add-skill eid skill])))

(defmethod entity/create! :entity/animation [[_ animation] eid]
  [[:tx/assoc eid :entity/image (animation/current-frame animation)]])

(defmethod entity/create! :entity/delete-after-animation-stopped? [_ eid]
  (-> @eid :entity/animation :looping? not assert)
  nil)

(def ^:private npc-fsm
  (fsm/fsm-inc
   [[:npc-sleeping
     :kill -> :npc-dead
     :stun -> :stunned
     :alert -> :npc-idle]
    [:npc-idle
     :kill -> :npc-dead
     :stun -> :stunned
     :start-action -> :active-skill
     :movement-direction -> :npc-moving]
    [:npc-moving
     :kill -> :npc-dead
     :stun -> :stunned
     :timer-finished -> :npc-idle]
    [:active-skill
     :kill -> :npc-dead
     :stun -> :stunned
     :action-done -> :npc-idle]
    [:stunned
     :kill -> :npc-dead
     :effect-wears-off -> :npc-idle]
    [:npc-dead]]))

(def ^:private player-fsm
  (fsm/fsm-inc
   [[:player-idle
     :kill -> :player-dead
     :stun -> :stunned
     :start-action -> :active-skill
     :pickup-item -> :player-item-on-cursor
     :movement-input -> :player-moving]
    [:player-moving
     :kill -> :player-dead
     :stun -> :stunned
     :no-movement-input -> :player-idle]
    [:active-skill
     :kill -> :player-dead
     :stun -> :stunned
     :action-done -> :player-idle]
    [:stunned
     :kill -> :player-dead
     :effect-wears-off -> :player-idle]
    [:player-item-on-cursor
     :kill -> :player-dead
     :stun -> :stunned
     :drop-item -> :player-idle
     :dropped-item -> :player-idle]
    [:player-dead]]))

(defmethod entity/create! :entity/fsm [[k {:keys [fsm initial-state]}] eid]
  ; fsm throws when initial-state is not part of states, so no need to assert initial-state
  ; initial state is nil, so associng it. make bug report at reduce-fsm?
  [[:tx/assoc eid k (assoc ((case fsm
                              :fsms/player player-fsm
                              :fsms/npc npc-fsm) initial-state nil) :state initial-state)]
   [:tx/assoc eid initial-state (entity/create [initial-state eid])]])

; this is not necessary if effect does not need target, but so far not other solution came up.
(defn- update-effect-ctx
  "Call this on effect-context if the time of using the context is not the time when context was built."
  [{:keys [effect/source effect/target] :as effect-ctx}]
  (if (and target
           (not (:entity/destroyed? @target))
           (world/line-of-sight? ctx/world @source @target))
    effect-ctx
    (dissoc effect-ctx :effect/target)))

(defmethod entity/tick! :active-skill [[_ {:keys [skill effect-ctx counter]}] eid]
  (cond
   (not (effect/some-applicable? (update-effect-ctx effect-ctx) ; TODO how 2 test
                                 (:skill/effects skill)))
   [[:tx/event eid :action-done]
    ; TODO some sound ?
    ]

   (timer/stopped? ctx/elapsed-time counter)
   [[:tx/effect effect-ctx (:skill/effects skill)]
    [:tx/event eid :action-done]]))

(defn- npc-choose-skill [entity ctx]
  (->> entity
       :entity/skills
       vals
       (sort-by #(or (:skill/cost %) 0))
       reverse
       (filter #(and (= :usable (skill-usable-state entity % ctx))
                     (effect/applicable-and-useful? ctx (:skill/effects %))))
       first))

(defn- npc-effect-context [eid]
  (let [entity @eid
        target (cell/nearest-entity @((:grid ctx/world) (entity/tile entity))
                                    (entity/enemy entity))
        target (when (and target
                          (world/line-of-sight? ctx/world entity @target))
                 target)]
    {:effect/source eid
     :effect/target target
     :effect/target-direction (when target
                                (entity/direction entity @target))}))

(defmethod entity/tick! :npc-idle [_ eid]
  (let [effect-ctx (npc-effect-context eid)]
    (if-let [skill (npc-choose-skill @eid effect-ctx)]
      [[:tx/event eid :start-action [skill effect-ctx]]]
      [[:tx/event eid :movement-direction (or (world/potential-field-direction ctx/world eid)
                                              [0 0])]])))

(defmethod entity/tick! :npc-moving [[_ {:keys [counter]}] eid]
  (when (timer/stopped? ctx/elapsed-time counter)
    [[:tx/event eid :timer-finished]]))

(defmethod entity/tick! :npc-sleeping [_ eid]
  (let [entity @eid
        cell ((:grid ctx/world) (entity/tile entity))]
    (when-let [distance (cell/nearest-entity-distance @cell (entity/enemy entity))]
      (when (<= distance (entity/stat entity :entity/aggro-range))
        [[:tx/event eid :alert]]))))

(defmethod entity/tick! :player-moving [[_ {:keys [movement-vector]}] eid]
  (if-let [movement-vector (input/player-movement-vector)]
    [[:tx/set-movement eid movement-vector]]
    [[:tx/event eid :no-movement-input]]))

(defmethod entity/tick! :stunned [[_ {:keys [counter]}] eid]
  (when (timer/stopped? ctx/elapsed-time counter)
    [[:tx/event eid :effect-wears-off]]))

(defmethod entity/tick! :entity/alert-friendlies-after-duration [[_ {:keys [counter faction]}] eid]
  (when (timer/stopped? ctx/elapsed-time counter)
    (cons [:tx/mark-destroyed eid]
          (for [friendly-eid (->> {:position (:position @eid)
                                   :radius 4}
                                  (grid/circle->entities (:grid ctx/world))
                                  (filter #(= (:entity/faction @%) faction)))]
            [:tx/event friendly-eid :alert]))))

(defmethod entity/tick! :entity/animation [[_ animation] eid]
  [[:tx/update-animation eid animation]])

(defn- move-position [position {:keys [direction speed delta-time]}]
  (mapv #(+ %1 (* %2 speed delta-time)) position direction))

(defn- move-body [body movement]
  (-> body
      (update :position    move-position movement)
      (update :left-bottom move-position movement)))

(defn- valid-position? [grid {:keys [entity/id z-order] :as body}]
  {:pre [(:collides? body)]}
  (let [cells* (into [] (map deref) (grid/rectangle->cells grid body))]
    (and (not-any? #(cell/blocked? % z-order) cells*)
         (->> cells*
              grid/cells->entities
              (not-any? (fn [other-entity]
                          (let [other-entity @other-entity]
                            (and (not= (:entity/id other-entity) id)
                                 (:collides? other-entity)
                                 (entity/collides? other-entity body)))))))))

(defn- try-move [grid body movement]
  (let [new-body (move-body body movement)]
    (when (valid-position? grid new-body)
      new-body)))

; TODO sliding threshold
; TODO name - with-sliding? 'on'
; TODO if direction was [-1 0] and invalid-position then this algorithm tried to move with
; direection [0 0] which is a waste of processor power...
(defn- try-move-solid-body [grid body {[vx vy] :direction :as movement}]
  (let [xdir (Math/signum (float vx))
        ydir (Math/signum (float vy))]
    (or (try-move grid body movement)
        (try-move grid body (assoc movement :direction [xdir 0]))
        (try-move grid body (assoc movement :direction [0 ydir])))))

; set max speed so small entities are not skipped by projectiles
; could set faster than max-speed if I just do multiple smaller movement steps in one frame
(def ^:private max-speed (/ ctx/minimum-size ctx/max-delta)) ; need to make var because s/schema would fail later if divide / is inside the schema-form

(def ^:private speed-schema (m/schema [:and number? [:>= 0] [:<= max-speed]]))

(defmethod entity/tick! :entity/movement [[_ {:keys [direction speed rotate-in-movement-direction?] :as movement}]
                                          eid]
  (assert (m/validate speed-schema speed)
          (pr-str speed))
  (assert (or (zero? (v/length direction))
              (v/normalised? direction))
          (str "cannot understand direction: " (pr-str direction)))
  (when-not (or (zero? (v/length direction))
                (nil? speed)
                (zero? speed))
    (let [movement (assoc movement :delta-time ctx/delta-time)
          body @eid]
      (when-let [body (if (:collides? body) ; < == means this is a movement-type ... which could be a multimethod ....
                        (try-move-solid-body (:grid ctx/world) body movement)
                        (move-body body movement))]
        [[:tx/move-entity eid body direction rotate-in-movement-direction?]]))))

(defmethod entity/tick! :entity/projectile-collision
  [[k {:keys [entity-effects already-hit-bodies piercing?]}] eid]
  ; TODO this could be called from body on collision
  ; for non-solid
  ; means non colliding with other entities
  ; but still collding with other stuff here ? o.o
  (let [entity @eid
        cells* (map deref (grid/rectangle->cells (:grid ctx/world) entity)) ; just use cached-touched -cells
        hit-entity (find-first #(and (not (contains? already-hit-bodies %)) ; not filtering out own id
                                     (not= (:entity/faction entity) ; this is not clear in the componentname & what if they dont have faction - ??
                                           (:entity/faction @%))
                                     (:collides? @%)
                                     (entity/collides? entity @%))
                               (grid/cells->entities cells*))
        destroy? (or (and hit-entity (not piercing?))
                     (some #(cell/blocked? % (:z-order entity)) cells*))]
    [(when destroy?
       [:tx/mark-destroyed eid])
     (when hit-entity
       [:tx/assoc-in eid [k :already-hit-bodies] (conj already-hit-bodies hit-entity)] ; this is only necessary in case of not piercing ...
       )
     (when hit-entity
       [:tx/effect {:effect/source eid :effect/target hit-entity} entity-effects])]))

(defmethod entity/tick! :entity/delete-after-animation-stopped? [_ eid]
  (when (animation/stopped? (:entity/animation @eid))
    [[:tx/mark-destroyed eid]]))

(defmethod entity/tick! :entity/skills [[k skills] eid]
  (for [{:keys [skill/cooling-down?] :as skill} (vals skills)
        :when (and cooling-down?
                   (timer/stopped? ctx/elapsed-time cooling-down?))]
    [:tx/assoc-in eid [k (:property/id skill) :skill/cooling-down?] false]))

(defmethod entity/tick! :entity/string-effect [[k {:keys [counter]}] eid]
  (when (timer/stopped? ctx/elapsed-time counter)
    [[:tx/dissoc eid k]]))

(defmethod entity/tick! :entity/temp-modifier [[k {:keys [modifiers counter]}] eid]
  (when (timer/stopped? ctx/elapsed-time counter)
    [[:tx/dissoc eid k]
     [:tx/mod-remove eid modifiers]]))

(defcomponent :active-skill
  (state/cursor [_] :cursors/sandclock)
  (state/pause-game? [_] false)
  (state/enter! [[_ {:keys [eid skill]}]]
    [[:tx/sound (:skill/start-action-sound skill)]
     (when (:skill/cooldown skill)
       [:tx/set-cooldown eid skill])
     (when (and (:skill/cost skill)
                (not (zero? (:skill/cost skill))))
       [:tx/pay-mana-cost eid (:skill/cost skill)])]))

(defcomponent :player-dead
  (state/cursor [_] :cursors/black-x)
  (state/pause-game? [_] true)
  (state/enter! [_]
    [[:tx/sound "bfxr_playerdeath"]
     [:tx/show-modal {:title "YOU DIED - again!"
                      :text "Good luck next time!"
                      :button-text "OK"
                      :on-click (fn [])}]]))

(defcomponent :player-moving
  (state/cursor [_] :cursors/walking)
  (state/pause-game? [_] false)
  (state/enter! [[_ {:keys [eid movement-vector]}]]
    [[:tx/set-movement eid movement-vector]])
  (state/exit! [[_ {:keys [eid]}]]
    [[:tx/dissoc eid :entity/movement]]))

(defcomponent :stunned
  (state/cursor [_] :cursors/denied)
  (state/pause-game? [_] false))

(defcomponent :entity/destroy-audiovisual
  (entity/destroy! [[_ audiovisuals-id] eid]
    [[:tx/audiovisual (:position @eid) (db/build ctx/db audiovisuals-id)]]))

(defcomponent :npc-dead
  (state/enter! [[_ {:keys [eid]}]]
    [[:tx/mark-destroyed eid]]))

(defcomponent :npc-moving
  (state/enter! [[_ {:keys [eid movement-vector]}]]
    [[:tx/set-movement eid movement-vector]])
  (state/exit! [[_ {:keys [eid]}]]
    [[:tx/dissoc eid :entity/movement]]))

(defcomponent :npc-sleeping
  (state/exit! [[_ {:keys [eid]}]]
    [[:tx/spawn-alert (:position @eid) (:entity/faction @eid) 0.2]
     [:tx/add-text-effect eid "[WHITE]!"]]))

(defn- draw-skill-image [image entity [x y] action-counter-ratio]
  (let [[width height] (:world-unit-dimensions image)
        _ (assert (= width height))
        radius (/ (float width) 2)
        y (+ (float y) (float (:half-height entity)) (float 0.15))
        center [x (+ y radius)]]
    (draw/filled-circle center radius [1 1 1 0.125])
    (draw/sector center
                 radius
                 90 ; start-angle
                 (* (float action-counter-ratio) 360) ; degree
                 [1 1 1 0.5])
    (draw/image image [(- (float x) radius) y])))

(def ^:private hpbar-colors
  {:green     [0 0.8 0]
   :darkgreen [0 0.5 0]
   :yellow    [0.5 0.5 0]
   :red       [0.5 0 0]})

(defn- hpbar-color [ratio]
  (let [ratio (float ratio)
        color (cond
               (> ratio 0.75) :green
               (> ratio 0.5)  :darkgreen
               (> ratio 0.25) :yellow
               :else          :red)]
    (color hpbar-colors)))

(def ^:private borders-px 1)

(defn- draw-hpbar [{:keys [position width half-width half-height]} ratio]
  (let [[x y] position]
    (let [x (- x half-width)
          y (+ y half-height)
          height (* 5 ctx/world-unit-scale)
          border (* borders-px ctx/world-unit-scale)]
      (draw/filled-rectangle x y width height :black)
      (draw/filled-rectangle (+ x border)
                             (+ y border)
                             (- (* width ratio) (* 2 border))
                             (- height          (* 2 border))
                             (hpbar-color ratio)))))

(defmethod entity/render-default! :entity/clickable
  [[_ {:keys [text]}] {:keys [entity/mouseover?] :as entity}]
  (when (and mouseover? text)
    (let [[x y] (:position entity)]
      (draw/text {:text text
                  :x x
                  :y (+ y (:half-height entity))
                  :up? true}))))

(defmethod entity/render-info! :entity/hp [_ entity]
  (let [ratio (val-max/ratio (entity/hitpoints entity))]
    (when (or (< ratio 1) (:entity/mouseover? entity))
      (draw-hpbar entity ratio))))

(defmethod entity/render-default! :entity/image [[_ image] entity]
  (draw/rotated-centered image
                         (or (:rotation-angle entity) 0)
                         (:position entity)))

(defmethod entity/render-default! :entity/line-render
  [[_ {:keys [thick? end color]}] entity]
  (let [position (:position entity)]
    (if thick?
      (draw/with-line-width 4
        #(draw/line position end color))
      (draw/line position end color))))

(def ^:private outline-alpha 0.4)
(def ^:private enemy-color    [1 0 0 outline-alpha])
(def ^:private friendly-color [0 1 0 outline-alpha])
(def ^:private neutral-color  [1 1 1 outline-alpha])

(defmethod entity/render-below! :entity/mouseover?
  [_ {:keys [entity/faction] :as entity}]
  (let [player @ctx/player-eid]
    (draw/with-line-width 3
      #(draw/ellipse (:position entity)
                     (:half-width entity)
                     (:half-height entity)
                     (cond (= faction (entity/enemy player))
                           enemy-color
                           (= faction (:entity/faction player))
                           friendly-color
                           :else
                           neutral-color)))))

(defn- render-active-effect [effect-ctx effect]
  (run! #(effect/render % effect-ctx) effect))

(defmethod entity/render-info! :active-skill
  [[_ {:keys [skill effect-ctx counter]}] entity]
  (let [{:keys [entity/image skill/effects]} skill]
    (draw-skill-image image
                      entity
                      (:position entity)
                      (timer/ratio ctx/elapsed-time counter))
    (render-active-effect effect-ctx ; TODO !!!
                          ; !! FIXME !!
                          ; (update-effect-ctx effect-ctx)
                          ; - render does not need to update .. update inside active-skill
                          effects)))

(defmethod entity/render-above! :npc-sleeping [_ entity]
  (let [[x y] (:position entity)]
    (draw/text {:text "zzz"
                :x x
                :y (+ y (:half-height entity))
                :up? true})))

(defmethod entity/render-below! :stunned [_ entity]
  (draw/circle (:position entity) 0.5 [1 1 1 0.6]))

(defmethod entity/render-above! :entity/string-effect [[_ {:keys [text]}] entity]
  (let [[x y] (:position entity)]
    (draw/text {:text text
                :x x
                :y (+ y
                      (:half-height entity)
                      (* 5 ctx/world-unit-scale))
                :scale 2
                :up? true})))

; TODO draw opacity as of counter ratio?
(defmethod entity/render-above! :entity/temp-modifier [_ entity]
  (draw/filled-circle (:position entity) 0.5 [0.5 0.5 0.5 0.4]))
