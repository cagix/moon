(ns cdq.impl.entity
  (:require [cdq.audio.sound :as sound]
            [cdq.ctx :as ctx]
            [cdq.data.val-max :as val-max]
            [cdq.db :as db]
            [cdq.db.schema :as schema]
            [cdq.effect :as effect]
            [cdq.entity :as entity]
            [cdq.entity.inventory :as inventory]
            [cdq.entity.skill :as skill]
            [cdq.entity.state :as state]
            [cdq.g :as g]
            [cdq.graphics :as graphics]
            [cdq.input :as input]
            [cdq.world :as world]
            [cdq.world.grid :as grid]
            [cdq.world.potential-field :as potential-field]
            [clojure.data.animation :as animation]
            [clojure.gdx :as gdx]
            [clojure.gdx.input :as gdx.input]
            [clojure.gdx.scene2d.actor :as actor]
            [clojure.gdx.scene2d.ui :as ui]
            [clojure.gdx.math.vector2 :as v]
            [clojure.timer :as timer]
            [clojure.utils :refer [defcomponent find-first]]
            [reduce-fsm :as fsm]))

(defmulti ^:private on-clicked
  (fn [eid]
    (:type (:entity/clickable @eid))))

(defmethod on-clicked :clickable/item [eid]
  (let [item (:entity/item @eid)]
    (cond
     (-> ctx/stage :windows :inventory-window actor/visible?)
     (do
      (sound/play! "bfxr_takeit")
      (entity/mark-destroyed eid)
      (entity/send-event! ctx/player-eid :pickup-item item))

     (inventory/can-pickup-item? (:entity/inventory @ctx/player-eid) item)
     (do
      (sound/play! "bfxr_pickup")
      (entity/mark-destroyed eid)
      (entity/pickup-item ctx/player-eid item))

     :else
     (do
      (sound/play! "bfxr_denied")
      (cdq.stage/show-message! ctx/stage "Your Inventory is full")))))

(defmethod on-clicked :clickable/player [_]
  (-> ctx/stage :windows :inventory-window actor/toggle-visible!))

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
    [(clickable->cursor @clicked-eid false) (fn []
                                              (on-clicked clicked-eid))]
    [(clickable->cursor @clicked-eid true)  (fn []
                                              (sound/play! "bfxr_denied")
                                              (cdq.stage/show-message! ctx/stage "Too far away"))]))

(defn- inventory-cell-with-item? [actor]
  (and (actor/parent actor)
       (= "inventory-cell" (actor/name (actor/parent actor)))
       (get-in (:entity/inventory @ctx/player-eid)
               (actor/user-object (actor/parent actor)))))

(defn- mouseover-actor->cursor []
  (let [actor (cdq.stage/mouse-on-actor? ctx/stage)]
    (cond
     (inventory-cell-with-item? actor) :cursors/hand-before-grab
     (ui/window-title-bar? actor)      :cursors/move-window
     (ui/button? actor)                :cursors/over-button
     :else                             :cursors/default)))

(defn- player-effect-ctx [eid]
  (let [target-position (or (and ctx/mouseover-eid
                                 (:position @ctx/mouseover-eid))
                            (graphics/world-mouse-position ctx/graphics))]
    {:effect/source eid
     :effect/target ctx/mouseover-eid
     :effect/target-position target-position
     :effect/target-direction (v/direction (:position @eid) target-position)}))

(defn- interaction-state [eid]
  (let [entity @eid]
    (cond
     (cdq.stage/mouse-on-actor? ctx/stage)
     [(mouseover-actor->cursor)
      (fn [] nil)] ; handled by actors themself, they check player state

     (and ctx/mouseover-eid
          (:entity/clickable @ctx/mouseover-eid))
     (clickable-entity-interaction entity ctx/mouseover-eid)

     :else
     (if-let [skill-id (cdq.stage/selected-skill ctx/stage)]
       (let [skill (skill-id (:entity/skills entity))
             effect-ctx (player-effect-ctx eid)
             state (skill/usable-state entity skill effect-ctx)]
         (if (= state :usable)
           (do
            ; TODO cursor AS OF SKILL effect (SWORD !) / show already what the effect would do ? e.g. if it would kill highlight
            ; different color ?
            ; => e.g. meditation no TARGET .. etc.
            [:cursors/use-skill
             (fn []
               (entity/send-event! eid :start-action [skill effect-ctx]))])
           (do
            ; TODO cursor as of usable state
            ; cooldown -> sanduhr kleine
            ; not-enough-mana x mit kreis?
            ; invalid-params -> depends on params ...
            [:cursors/skill-not-usable
             (fn []
               (sound/play! "bfxr_denied")
               (cdq.stage/show-message! ctx/stage (case state
                                                       :cooldown "Skill is still on cooldown"
                                                       :not-enough-mana "Not enough mana"
                                                       :invalid-params "Cannot use this here")))])))
       [:cursors/no-skill-selected
        (fn []
          (sound/play! "bfxr_denied")
          (cdq.stage/show-message! ctx/stage "No selected skill"))]))))

(defcomponent :player-idle
  (entity/create [[_ eid]]
    {:eid eid})

  (state/pause-game? [_] true)

  (state/manual-tick [[_ {:keys [eid]}]]
    (if-let [movement-vector (input/player-movement-vector)]
      (entity/send-event! eid :movement-input movement-vector)
      (let [[cursor on-click] (interaction-state eid)]
        (graphics/set-cursor! ctx/graphics cursor)
        (when (gdx.input/button-just-pressed? gdx/input :left)
          (on-click)))))

  (state/clicked-inventory-cell [[_ {:keys [eid]}] cell]
    ; TODO no else case
    (when-let [item (get-in (:entity/inventory @eid) cell)]
      (sound/play! "bfxr_takeit")
      (entity/send-event! eid :pickup-item item)
      (entity/remove-item eid cell))))

(defn- clicked-cell [eid cell]
  (let [entity @eid
        inventory (:entity/inventory entity)
        item-in-cell (get-in inventory cell)
        item-on-cursor (:entity/item-on-cursor entity)]
    (cond
     ; PUT ITEM IN EMPTY CELL
     (and (not item-in-cell)
          (inventory/valid-slot? cell item-on-cursor))
     (do
      (sound/play! "bfxr_itemput")
      (swap! eid dissoc :entity/item-on-cursor)
      (entity/set-item eid cell item-on-cursor)
      (entity/send-event! eid :dropped-item))

     ; STACK ITEMS
     (and item-in-cell
          (inventory/stackable? item-in-cell item-on-cursor))
     (do
      (sound/play! "bfxr_itemput")
      (swap! eid dissoc :entity/item-on-cursor)
      (entity/stack-item eid cell item-on-cursor)
      (entity/send-event! eid :dropped-item))

     ; SWAP ITEMS
     (and item-in-cell
          (inventory/valid-slot? cell item-on-cursor))
     (do
      (sound/play! "bfxr_itemput")
      ; need to dissoc and drop otherwise state enter does not trigger picking it up again
      ; TODO? coud handle pickup-item from item-on-cursor state also
      (swap! eid dissoc :entity/item-on-cursor)
      (entity/remove-item eid cell)
      (entity/set-item eid cell item-on-cursor)
      (entity/send-event! eid :dropped-item)
      (entity/send-event! eid :pickup-item item-in-cell)))))

(defcomponent :player-item-on-cursor
  (entity/create [[_ eid item]]
    {:eid eid
     :item item})

  (entity/render-below! [[_ {:keys [item]}] entity g]
    (when (g/world-item?)
      (graphics/draw-centered g
                              (:entity/image item)
                              (g/item-place-position entity))))

  (state/cursor [_] :cursors/hand-grab)

  (state/pause-game? [_] true)

  (state/enter! [[_ {:keys [eid item]}]]
    (swap! eid assoc :entity/item-on-cursor item))

  (state/exit! [[_ {:keys [eid]}]]
    ; at clicked-cell when we put it into a inventory-cell
    ; we do not want to drop it on the ground too additonally,
    ; so we dissoc it there manually. Otherwise it creates another item
    ; on the ground
    (let [entity @eid]
      (when (:entity/item-on-cursor entity)
        (sound/play! "bfxr_itemputground")
        (swap! eid dissoc :entity/item-on-cursor)
        (g/spawn-item (g/item-place-position entity)
                      (:entity/item-on-cursor entity)))))

  (state/manual-tick [[_ {:keys [eid]}]]
    (when (and (gdx.input/button-just-pressed? gdx/input :left)
               (g/world-item?))
      (entity/send-event! eid :drop-item)))

  (state/clicked-inventory-cell [[_ {:keys [eid]}] cell]
    (clicked-cell eid cell))

  (state/draw-gui-view [[_ {:keys [eid]}]]
    (when (not (g/world-item?))
      (graphics/draw-centered ctx/graphics
                              (:entity/image (:entity/item-on-cursor @eid))
                              (graphics/mouse-position ctx/graphics)))))

(defcomponent :entity/delete-after-duration
  (entity/create [[_ duration]]
    (timer/create ctx/elapsed-time duration))

  (entity/tick! [[_ counter] eid]
    (when (timer/stopped? ctx/elapsed-time counter)
      (entity/mark-destroyed eid))))

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
  (swap! eid assoc k inventory/empty-inventory)
  (doseq [item items]
    (entity/pickup-item eid item)))

(defmethod entity/create! :entity/skills [[k skills] eid]
  (swap! eid assoc k nil)
  (doseq [skill skills]
    (entity/add-skill eid skill)))

(defmethod entity/create! :entity/animation [[_ animation] eid]
  (swap! eid assoc :entity/image (animation/current-frame animation)))

(defmethod entity/create! :entity/delete-after-animation-stopped? [_ eid]
  (-> @eid :entity/animation :looping? not assert))

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
  (swap! eid assoc
         ; fsm throws when initial-state is not part of states, so no need to assert initial-state
         ; initial state is nil, so associng it. make bug report at reduce-fsm?
         k (assoc ((case fsm
                     :fsms/player player-fsm
                     :fsms/npc npc-fsm) initial-state nil) :state initial-state)
         initial-state (entity/create [initial-state eid])))

; this is not necessary if effect does not need target, but so far not other solution came up.
(defn- update-effect-ctx
  "Call this on effect-context if the time of using the context is not the time when context was built."
  [{:keys [effect/source effect/target] :as effect-ctx}]
  (if (and target
           (not (:entity/destroyed? @target))
           (g/line-of-sight? @source @target))
    effect-ctx
    (dissoc effect-ctx :effect/target)))

(defmethod entity/tick! :active-skill [[_ {:keys [skill effect-ctx counter]}] eid]
  (cond
   (not (effect/some-applicable? (update-effect-ctx effect-ctx)
                                 (:skill/effects skill)))
   (do
    (entity/send-event! eid :action-done)
    ; TODO some sound ?
    )

   (timer/stopped? ctx/elapsed-time counter)
   (do
    (effect/do-all! effect-ctx (:skill/effects skill))
    (entity/send-event! eid :action-done))))

(defn- npc-choose-skill [entity ctx]
  (->> entity
       :entity/skills
       vals
       (sort-by #(or (:skill/cost %) 0))
       reverse
       (filter #(and (= :usable (skill/usable-state entity % ctx))
                     (effect/applicable-and-useful? ctx (:skill/effects %))))
       first))

(defn- npc-effect-context [eid]
  (let [entity @eid
        target (g/nearest-enemy ctx/world entity)
        target (when (and target
                          (g/line-of-sight? entity @target))
                 target)]
    {:effect/source eid
     :effect/target target
     :effect/target-direction (when target
                                (entity/direction entity @target))}))

(defmethod entity/tick! :npc-idle [_ eid]
  (let [effect-ctx (npc-effect-context eid)]
    (if-let [skill (npc-choose-skill @eid effect-ctx)]
      (entity/send-event! eid :start-action [skill effect-ctx])
      (entity/send-event! eid :movement-direction (or (potential-field/find-direction (:grid ctx/world) eid) [0 0])))))

(defmethod entity/tick! :npc-moving [[_ {:keys [counter]}] eid]
  (when (timer/stopped? ctx/elapsed-time counter)
    (entity/send-event! eid :timer-finished)))

(defmethod entity/tick! :npc-sleeping [_ eid]
  (let [entity @eid
        cell ((:grid ctx/world) (entity/tile entity))]
    (when-let [distance (grid/nearest-entity-distance @cell (entity/enemy entity))]
      (when (<= distance (entity/stat entity :entity/aggro-range))
        (entity/send-event! eid :alert)))))

(defmethod entity/tick! :player-moving [[_ {:keys [movement-vector]}] eid]
  (if-let [movement-vector (input/player-movement-vector)]
    (entity/set-movement eid movement-vector)
    (entity/send-event! eid :no-movement-input)))

(defmethod entity/tick! :stunned [[_ {:keys [counter]}] eid]
  (when (timer/stopped? ctx/elapsed-time counter)
    (entity/send-event! eid :effect-wears-off)))

(defmethod entity/tick! :entity/alert-friendlies-after-duration [[_ {:keys [counter faction]}] eid]
  (when (timer/stopped? ctx/elapsed-time counter)
    (entity/mark-destroyed eid)
    (doseq [friendly-eid (g/friendlies-in-radius (:grid ctx/world) (:position @eid) faction)]
      (entity/send-event! friendly-eid :alert))))

(defmethod entity/tick! :entity/animation [[k animation] eid]
  (swap! eid #(-> %
                  (assoc :entity/image (animation/current-frame animation))
                  (assoc k (animation/tick animation ctx/delta-time)))))

(defn- move-position [position {:keys [direction speed delta-time]}]
  (mapv #(+ %1 (* %2 speed delta-time)) position direction))

(defn- move-body [body movement]
  (-> body
      (update :position    move-position movement)
      (update :left-bottom move-position movement)))

(defn- valid-position? [grid {:keys [entity/id z-order] :as body}]
  {:pre [(:collides? body)]}
  (let [cells* (into [] (map deref) (grid/rectangle->cells grid body))]
    (and (not-any? #(grid/blocked? % z-order) cells*)
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
(def ^:private max-speed (/ g/minimum-size g/max-delta)) ; need to make var because s/schema would fail later if divide / is inside the schema-form

(def ^:private speed-schema (schema/m-schema [:and number? [:>= 0] [:<= max-speed]]))

(defmethod entity/tick! :entity/movement [[_ {:keys [direction speed rotate-in-movement-direction?] :as movement}]
                                          eid]
  (assert (schema/validate speed-schema speed)
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
        (g/position-changed! ctx/world eid)
        (swap! eid assoc
               :position (:position body)
               :left-bottom (:left-bottom body))
        (when rotate-in-movement-direction?
          (swap! eid assoc :rotation-angle (v/angle-from-vector direction)))))))

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
                     (some #(grid/blocked? % (:z-order entity)) cells*))]
    (when destroy?
      (entity/mark-destroyed eid))
    (when hit-entity
      (swap! eid assoc-in [k :already-hit-bodies] (conj already-hit-bodies hit-entity))) ; this is only necessary in case of not piercing ...
    (when hit-entity
      (effect/do-all! {:effect/source eid
                       :effect/target hit-entity}
                      entity-effects))))

(defmethod entity/tick! :entity/delete-after-animation-stopped? [_ eid]
  (when (animation/stopped? (:entity/animation @eid))
    (entity/mark-destroyed eid)))

(defmethod entity/tick! :entity/skills [[k skills] eid]
  (doseq [{:keys [skill/cooling-down?] :as skill} (vals skills)
          :when (and cooling-down?
                     (timer/stopped? ctx/elapsed-time cooling-down?))]
    (swap! eid assoc-in [k (:property/id skill) :skill/cooling-down?] false)))

(defmethod entity/tick! :entity/string-effect [[k {:keys [counter]}] eid]
  (when (timer/stopped? ctx/elapsed-time counter)
    (swap! eid dissoc k)))

(defmethod entity/tick! :entity/temp-modifier [[k {:keys [modifiers counter]}] eid]
  (when (timer/stopped? ctx/elapsed-time counter)
    (swap! eid dissoc k)
    (swap! eid entity/mod-remove modifiers)))

(defcomponent :active-skill
  (state/cursor [_] :cursors/sandclock)
  (state/pause-game? [_] false)
  (state/enter! [[_ {:keys [eid skill]}]]
    (sound/play! (:skill/start-action-sound skill))
    (when (:skill/cooldown skill)
      (swap! eid assoc-in
             [:entity/skills (:property/id skill) :skill/cooling-down?]
             (timer/create ctx/elapsed-time (:skill/cooldown skill))))
    (when (and (:skill/cost skill)
               (not (zero? (:skill/cost skill))))
      (swap! eid entity/pay-mana-cost (:skill/cost skill)))))

(defcomponent :player-dead
  (state/cursor [_] :cursors/black-x)
  (state/pause-game? [_] true)
  (state/enter! [_]
    (sound/play! "bfxr_playerdeath")
    (cdq.stage/show-modal! ctx/stage {:title "YOU DIED - again!"
                                      :text "Good luck next time!"
                                      :button-text "OK"
                                      :on-click (fn [])})))

(defcomponent :player-moving
  (state/cursor [_] :cursors/walking)
  (state/pause-game? [_] false)
  (state/enter! [[_ {:keys [eid movement-vector]}]]
    (entity/set-movement eid movement-vector))
  (state/exit! [[_ {:keys [eid]}]]
    (swap! eid dissoc :entity/movement)))

(defcomponent :stunned
  (state/cursor [_] :cursors/denied)
  (state/pause-game? [_] false))

(defcomponent :entity/destroy-audiovisual
  (entity/destroy! [[_ audiovisuals-id] eid]
    (g/spawn-audiovisual (:position @eid) (db/build ctx/db audiovisuals-id))))

(defcomponent :npc-dead
  (state/enter! [[_ {:keys [eid]}]]
    (entity/mark-destroyed eid)))

(defcomponent :npc-moving
  (state/enter! [[_ {:keys [eid movement-vector]}]]
    (entity/set-movement eid movement-vector))
  (state/exit! [[_ {:keys [eid]}]]
    (swap! eid dissoc :entity/movement)))

(defcomponent :npc-sleeping
  (state/exit! [[_ {:keys [eid]}]]
    (g/delayed-alert (:position       @eid)
                     (:entity/faction @eid)
                     0.2)
    (entity/add-text-effect! eid "[WHITE]!")))

(defn- draw-skill-image [image entity [x y] action-counter-ratio g]
  (let [[width height] (:world-unit-dimensions image)
        _ (assert (= width height))
        radius (/ (float width) 2)
        y (+ (float y) (float (:half-height entity)) (float 0.15))
        center [x (+ y radius)]]
    (graphics/draw-filled-circle g center radius [1 1 1 0.125])
    (graphics/draw-sector g center
                          radius
                          90 ; start-angle
                          (* (float action-counter-ratio) 360) ; degree
                          [1 1 1 0.5])
    (graphics/draw-image g image [(- (float x) radius) y])))

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

(defn- draw-hpbar [{:keys [position width half-width half-height]} ratio g]
  (let [[x y] position]
    (let [x (- x half-width)
          y (+ y half-height)
          height (graphics/pixels->world-units g 5)
          border (graphics/pixels->world-units g borders-px)]
      (graphics/draw-filled-rectangle g x y width height :black)
      (graphics/draw-filled-rectangle g
                                      (+ x border)
                                      (+ y border)
                                      (- (* width ratio) (* 2 border))
                                      (- height          (* 2 border))
                                      (hpbar-color ratio)))))

(defmethod entity/render-default! :entity/clickable
  [[_ {:keys [text]}] {:keys [entity/mouseover?] :as entity} g]
  (when (and mouseover? text)
    (let [[x y] (:position entity)]
      (graphics/draw-text g
                          {:text text
                           :x x
                           :y (+ y (:half-height entity))
                           :up? true}))))

(defmethod entity/render-info! :entity/hp [_ entity g]
  (let [ratio (val-max/ratio (entity/hitpoints entity))]
    (when (or (< ratio 1) (:entity/mouseover? entity))
      (draw-hpbar entity ratio g))))

(defmethod entity/render-default! :entity/image
  [[_ image] entity g]
  (graphics/draw-rotated-centered g
                                  image
                                  (or (:rotation-angle entity) 0)
                                  (:position entity)))

(defmethod entity/render-default! :entity/line-render
  [[_ {:keys [thick? end color]}] entity g]
  (let [position (:position entity)]
    (if thick?
      (graphics/with-line-width g 4
        #(graphics/draw-line g position end color))
      (graphics/draw-line g position end color))))

(def ^:private outline-alpha 0.4)
(def ^:private enemy-color    [1 0 0 outline-alpha])
(def ^:private friendly-color [0 1 0 outline-alpha])
(def ^:private neutral-color  [1 1 1 outline-alpha])

(defmethod entity/render-below! :entity/mouseover?
  [_ {:keys [entity/faction] :as entity} g]
  (let [player @ctx/player-eid]
    (graphics/with-line-width g 3
      #(graphics/draw-ellipse g
                              (:position entity)
                              (:half-width entity)
                              (:half-height entity)
                              (cond (= faction (entity/enemy player))
                                    enemy-color
                                    (= faction (:entity/faction player))
                                    friendly-color
                                    :else
                                    neutral-color)))))

(defn- render-active-effect [effect-ctx effect g]
  (run! #(effect/render % effect-ctx g) effect))

(defmethod entity/render-info! :active-skill
  [[_ {:keys [skill effect-ctx counter]}] entity g]
  (let [{:keys [entity/image skill/effects]} skill]
    (draw-skill-image image
                      entity
                      (:position entity)
                      (timer/ratio ctx/elapsed-time counter)
                      g)
    (render-active-effect effect-ctx ; TODO !!!
                          ; !! FIXME !!
                          ; (update-effect-ctx effect-ctx)
                          ; - render does not need to update .. update inside active-skill
                          effects
                          g)))

(defmethod entity/render-above! :npc-sleeping [_ entity g]
  (let [[x y] (:position entity)]
    (graphics/draw-text g {:text "zzz"
                           :x x
                           :y (+ y (:half-height entity))
                           :up? true})))

(defmethod entity/render-below! :stunned [_ entity g]
  (graphics/draw-circle g (:position entity) 0.5 [1 1 1 0.6]))

(defmethod entity/render-above! :entity/string-effect [[_ {:keys [text]}] entity g]
  (let [[x y] (:position entity)]
    (graphics/draw-text g {:text text
                           :x x
                           :y (+ y
                                 (:half-height entity)
                                 (graphics/pixels->world-units g 5))
                           :scale 2
                           :up? true})))

; TODO draw opacity as of counter ratio?
(defmethod entity/render-above! :entity/temp-modifier [_ entity g]
  (graphics/draw-filled-circle g (:position entity) 0.5 [0.5 0.5 0.5 0.4]))
