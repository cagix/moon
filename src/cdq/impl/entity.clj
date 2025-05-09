(ns cdq.impl.entity
  (:require [cdq.effect :as effect]
            [cdq.entity :as entity]
            [cdq.entity.state :as state]
            [cdq.g :as g]
            [cdq.grid :as grid]
            [cdq.input :as input]
            [cdq.inventory :as inventory]
            [cdq.schema :as schema]
            [cdq.skill :as skill]
            [cdq.val-max :as val-max]
            [cdq.world.potential-field :as potential-field]
            [clojure.data.animation :as animation]
            [clojure.gdx :as gdx]
            [clojure.gdx.scene2d.actor :as actor]
            [clojure.gdx.scene2d.ui :as ui]
            [clojure.gdx.math.vector2 :as v]
            [clojure.utils :refer [defcomponent find-first]]
            [reduce-fsm :as fsm]))

(defmulti ^:private on-clicked
  (fn [eid]
    (:type (:entity/clickable @eid))))

(defmethod on-clicked :clickable/item [eid]
  (let [item (:entity/item @eid)]
    (cond
     (actor/visible? (g/get-inventory))
     (do
      (g/play-sound! "bfxr_takeit")
      (g/mark-destroyed eid)
      (g/send-event! g/player-eid :pickup-item item))

     (inventory/can-pickup-item? (:entity/inventory @g/player-eid) item)
     (do
      (g/play-sound! "bfxr_pickup")
      (g/mark-destroyed eid)
      (g/pickup-item g/player-eid item))

     :else
     (do
      (g/play-sound! "bfxr_denied")
      (g/show-player-msg! "Your Inventory is full")))))

(defmethod on-clicked :clickable/player [_]
  (g/toggle-inventory-window))

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
                                              (g/play-sound! "bfxr_denied")
                                              (g/show-player-msg! "Too far away"))]))

(defn- inventory-cell-with-item? [actor]
  (and (actor/parent actor)
       (= "inventory-cell" (actor/name (actor/parent actor)))
       (get-in (:entity/inventory @g/player-eid)
               (actor/user-object (actor/parent actor)))))

(defn- mouseover-actor->cursor []
  (let [actor (g/mouse-on-actor?)]
    (cond
     (inventory-cell-with-item? actor) :cursors/hand-before-grab
     (ui/window-title-bar? actor)      :cursors/move-window
     (ui/button? actor)                :cursors/over-button
     :else                             :cursors/default)))

(defn- player-effect-ctx [eid]
  (let [target-position (or (and g/mouseover-eid
                                 (:position @g/mouseover-eid))
                            (g/world-mouse-position))]
    {:effect/source eid
     :effect/target g/mouseover-eid
     :effect/target-position target-position
     :effect/target-direction (v/direction (:position @eid) target-position)}))

(defn- interaction-state [eid]
  (let [entity @eid]
    (cond
     (g/mouse-on-actor?)
     [(mouseover-actor->cursor)
      (fn [] nil)] ; handled by actors themself, they check player state

     (and g/mouseover-eid
          (:entity/clickable @g/mouseover-eid))
     (clickable-entity-interaction entity g/mouseover-eid)

     :else
     (if-let [skill-id (g/selected-skill)]
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
               (g/send-event! eid :start-action [skill effect-ctx]))])
           (do
            ; TODO cursor as of usable state
            ; cooldown -> sanduhr kleine
            ; not-enough-mana x mit kreis?
            ; invalid-params -> depends on params ...
            [:cursors/skill-not-usable
             (fn []
               (g/play-sound! "bfxr_denied")
               (g/show-player-msg! (case state
                                     :cooldown "Skill is still on cooldown"
                                     :not-enough-mana "Not enough mana"
                                     :invalid-params "Cannot use this here")))])))
       [:cursors/no-skill-selected
        (fn []
          (g/play-sound! "bfxr_denied")
          (g/show-player-msg! "No selected skill"))]))))

(defmethod entity/manual-tick :player-idle [[_ {:keys [eid]}]]
  (if-let [movement-vector (input/player-movement-vector)]
    (g/send-event! eid :movement-input movement-vector)
    (let [[cursor on-click] (interaction-state eid)]
      (g/set-cursor! cursor)
      (when (gdx/button-just-pressed? :left)
        (on-click)))))

(defmethod entity/manual-tick :player-item-on-cursor [[_ {:keys [eid]}]]
  (when (and (gdx/button-just-pressed? :left)
             (g/world-item?))
    (g/send-event! eid :drop-item)))

(defcomponent :entity/delete-after-duration
  (entity/create [[_ duration]]
    (g/->timer duration))

  (entity/tick! [[_ counter] eid]
    (when (g/stopped? counter)
      (g/mark-destroyed eid))))

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
                 g/->timer)})

(defmethod entity/create :npc-dead [[_ eid]]
  {:eid eid})

(defmethod entity/create :npc-idle [[_ eid]]
  {:eid eid})

(defmethod entity/create :npc-moving [[_ eid movement-vector]]
  {:eid eid
   :movement-vector movement-vector
   :counter (g/->timer (* (entity/stat @eid :entity/reaction-time) 0.016))})

(defmethod entity/create :npc-sleeping [[_ eid]]
  {:eid eid})

(defmethod entity/create :player-idle [[_ eid]]
  {:eid eid})

(defmethod entity/create :player-item-on-cursor [[_ eid item]]
  {:eid eid
   :item item})

(defmethod entity/create :player-moving [[_ eid movement-vector]]
  {:eid eid
   :movement-vector movement-vector})

(defmethod entity/create :stunned [[_ eid duration]]
  {:eid eid
   :counter (g/->timer duration)})

(defmethod entity/create! :entity/inventory [[k items] eid]
  (swap! eid assoc k inventory/empty-inventory)
  (doseq [item items]
    (g/pickup-item eid item)))

(defmethod entity/create! :entity/skills [[k skills] eid]
  (swap! eid assoc k nil)
  (doseq [skill skills]
    (g/add-skill eid skill)))

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

(defmethod entity/draw-gui-view :player-item-on-cursor [[_ {:keys [eid]}]]
  (when (not (g/world-item?))
    (g/draw-centered (:entity/image (:entity/item-on-cursor @eid))
                            (g/mouse-position))))

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
    (g/send-event! eid :action-done)
    ; TODO some sound ?
    )

   (g/stopped? counter)
   (do
    (effect/do-all! effect-ctx (:skill/effects skill))
    (g/send-event! eid :action-done))))

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
        target (g/nearest-enemy entity)
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
      (g/send-event! eid :start-action [skill effect-ctx])
      (g/send-event! eid :movement-direction (or (potential-field/find-direction g/grid eid) [0 0])))))

(defmethod entity/tick! :npc-moving [[_ {:keys [counter]}] eid]
  (when (g/stopped? counter)
    (g/send-event! eid :timer-finished)))

(defmethod entity/tick! :npc-sleeping [_ eid]
  (let [entity @eid
        cell (g/grid (entity/tile entity))]
    (when-let [distance (grid/nearest-entity-distance @cell (entity/enemy entity))]
      (when (<= distance (entity/stat entity :entity/aggro-range))
        (g/send-event! eid :alert)))))

(defmethod entity/tick! :player-moving [[_ {:keys [movement-vector]}] eid]
  (if-let [movement-vector (input/player-movement-vector)]
    (g/set-movement eid movement-vector)
    (g/send-event! eid :no-movement-input)))

(defmethod entity/tick! :stunned [[_ {:keys [counter]}] eid]
  (when (g/stopped? counter)
    (g/send-event! eid :effect-wears-off)))

(defmethod entity/tick! :entity/alert-friendlies-after-duration [[_ {:keys [counter faction]}] eid]
  (when (g/stopped? counter)
    (g/mark-destroyed eid)
    (doseq [friendly-eid (g/friendlies-in-radius g/grid (:position @eid) faction)]
      (g/send-event! friendly-eid :alert))))

(defmethod entity/tick! :entity/animation [[k animation] eid]
  (swap! eid #(-> %
                  (assoc :entity/image (animation/current-frame animation))
                  (assoc k (animation/tick animation g/delta-time)))))

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
    (let [movement (assoc movement :delta-time g/delta-time)
          body @eid]
      (when-let [body (if (:collides? body) ; < == means this is a movement-type ... which could be a multimethod ....
                        (try-move-solid-body g/grid body movement)
                        (move-body body movement))]
        (g/position-changed! eid)
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
        cells* (map deref (grid/rectangle->cells g/grid entity)) ; just use cached-touched -cells
        hit-entity (find-first #(and (not (contains? already-hit-bodies %)) ; not filtering out own id
                                     (not= (:entity/faction entity) ; this is not clear in the componentname & what if they dont have faction - ??
                                           (:entity/faction @%))
                                     (:collides? @%)
                                     (entity/collides? entity @%))
                               (grid/cells->entities cells*))
        destroy? (or (and hit-entity (not piercing?))
                     (some #(grid/blocked? % (:z-order entity)) cells*))]
    (when destroy?
      (g/mark-destroyed eid))
    (when hit-entity
      (swap! eid assoc-in [k :already-hit-bodies] (conj already-hit-bodies hit-entity))) ; this is only necessary in case of not piercing ...
    (when hit-entity
      (effect/do-all! {:effect/source eid
                       :effect/target hit-entity}
                      entity-effects))))

(defmethod entity/tick! :entity/delete-after-animation-stopped? [_ eid]
  (when (animation/stopped? (:entity/animation @eid))
    (g/mark-destroyed eid)))

(defmethod entity/tick! :entity/skills [[k skills] eid]
  (doseq [{:keys [skill/cooling-down?] :as skill} (vals skills)
          :when (and cooling-down?
                     (g/stopped? cooling-down?))]
    (swap! eid assoc-in [k (:property/id skill) :skill/cooling-down?] false)))

(defmethod entity/tick! :entity/string-effect [[k {:keys [counter]}] eid]
  (when (g/stopped? counter)
    (swap! eid dissoc k)))

(defmethod entity/tick! :entity/temp-modifier [[k {:keys [modifiers counter]}] eid]
  (when (g/stopped? counter)
    (swap! eid dissoc k)
    (swap! eid entity/mod-remove modifiers)))

(defcomponent :active-skill
  (state/cursor [_] :cursors/sandclock)
  (state/pause-game? [_] false)
  (state/enter! [[_ {:keys [eid skill]}]]
    (g/play-sound! (:skill/start-action-sound skill))
    (when (:skill/cooldown skill)
      (swap! eid assoc-in
             [:entity/skills (:property/id skill) :skill/cooling-down?]
             (g/->timer (:skill/cooldown skill))))
    (when (and (:skill/cost skill)
               (not (zero? (:skill/cost skill))))
      (swap! eid entity/pay-mana-cost (:skill/cost skill)))))

(defcomponent :player-idle
  (state/pause-game? [_] true))

(defcomponent :player-dead
  (state/cursor [_] :cursors/black-x)
  (state/pause-game? [_] true)
  (state/enter! [_]
    (g/play-sound! "bfxr_playerdeath")
    (g/show-modal {:title "YOU DIED - again!"
                    :text "Good luck next time!"
                    :button-text "OK"
                    :on-click (fn [])})))

(defcomponent :player-item-on-cursor
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
        (g/play-sound! "bfxr_itemputground")
        (swap! eid dissoc :entity/item-on-cursor)
        (g/spawn-item (g/item-place-position entity)
                          (:entity/item-on-cursor entity))))))

(defcomponent :player-moving
  (state/cursor [_] :cursors/walking)
  (state/pause-game? [_] false)
  (state/enter! [[_ {:keys [eid movement-vector]}]]
    (g/set-movement eid movement-vector))
  (state/exit! [[_ {:keys [eid]}]]
    (swap! eid dissoc :entity/movement)))

(defcomponent :stunned
  (state/cursor [_] :cursors/denied)
  (state/pause-game? [_] false))

(defcomponent :entity/destroy-audiovisual
  (entity/destroy! [[_ audiovisuals-id] eid]
    (g/spawn-audiovisual (:position @eid) (g/build audiovisuals-id))))

(defcomponent :npc-dead
  (state/enter! [[_ {:keys [eid]}]]
    (g/mark-destroyed eid)))

(defcomponent :npc-moving
  (state/enter! [[_ {:keys [eid movement-vector]}]]
    (g/set-movement eid movement-vector))
  (state/exit! [[_ {:keys [eid]}]]
    (swap! eid dissoc :entity/movement)))

(defcomponent :npc-sleeping
  (state/exit! [[_ {:keys [eid]}]]
    (g/delayed-alert (:position       @eid)
                     (:entity/faction @eid)
                     0.2)
    (g/add-text-effect! eid "[WHITE]!")))

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
      (g/play-sound! "bfxr_itemput")
      (swap! eid dissoc :entity/item-on-cursor)
      (g/set-item eid cell item-on-cursor)
      (g/send-event! eid :dropped-item))

     ; STACK ITEMS
     (and item-in-cell
          (inventory/stackable? item-in-cell item-on-cursor))
     (do
      (g/play-sound! "bfxr_itemput")
      (swap! eid dissoc :entity/item-on-cursor)
      (g/stack-item eid cell item-on-cursor)
      (g/send-event! eid :dropped-item))

     ; SWAP ITEMS
     (and item-in-cell
          (inventory/valid-slot? cell item-on-cursor))
     (do
      (g/play-sound! "bfxr_itemput")
      ; need to dissoc and drop otherwise state enter does not trigger picking it up again
      ; TODO? coud handle pickup-item from item-on-cursor state also
      (swap! eid dissoc :entity/item-on-cursor)
      (g/remove-item eid cell)
      (g/set-item eid cell item-on-cursor)
      (g/send-event! eid :dropped-item)
      (g/send-event! eid :pickup-item item-in-cell)))))

(defmethod entity/clicked-inventory-cell :player-item-on-cursor
  [[_ {:keys [eid]}] cell]
  (clicked-cell eid cell))

(defmethod entity/clicked-inventory-cell :player-idle
  [[_ {:keys [eid]}] cell]
  ; TODO no else case
  (when-let [item (get-in (:entity/inventory @eid) cell)]
    (g/play-sound! "bfxr_takeit")
    (g/send-event! eid :pickup-item item)
    (g/remove-item eid cell)))

(defn- draw-skill-image [image entity [x y] action-counter-ratio]
  (let [[width height] (:world-unit-dimensions image)
        _ (assert (= width height))
        radius (/ (float width) 2)
        y (+ (float y) (float (:half-height entity)) (float 0.15))
        center [x (+ y radius)]]
    (g/draw-filled-circle center radius [1 1 1 0.125])
    (g/draw-sector center
                   radius
                   90 ; start-angle
                   (* (float action-counter-ratio) 360) ; degree
                   [1 1 1 0.5])
    (g/draw-image image [(- (float x) radius) y])))

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
          height (g/pixels->world-units 5)
          border (g/pixels->world-units borders-px)]
      (g/draw-filled-rectangle x y width height :black)
      (g/draw-filled-rectangle (+ x border)
                               (+ y border)
                               (- (* width ratio) (* 2 border))
                               (- height          (* 2 border))
                               (hpbar-color ratio)))))

(defmethod entity/render-default! :entity/clickable
  [[_ {:keys [text]}] {:keys [entity/mouseover?] :as entity}]
  (when (and mouseover? text)
    (let [[x y] (:position entity)]
      (g/draw-text {:text text
                    :x x
                    :y (+ y (:half-height entity))
                    :up? true}))))

(defmethod entity/render-info! :entity/hp
  [_ entity]
  (let [ratio (val-max/ratio (entity/hitpoints entity))]
    (when (or (< ratio 1) (:entity/mouseover? entity))
      (draw-hpbar entity ratio))))

(defmethod entity/render-default! :entity/image
  [[_ image] entity]
  (g/draw-rotated-centered image
                           (or (:rotation-angle entity) 0)
                           (:position entity)))

(defmethod entity/render-default! :entity/line-render
  [[_ {:keys [thick? end color]}] entity]
  (let [position (:position entity)]
    (if thick?
      (g/with-line-width 4
        #(g/draw-line position end color))
      (g/draw-line position end color))))

(def ^:private outline-alpha 0.4)
(def ^:private enemy-color    [1 0 0 outline-alpha])
(def ^:private friendly-color [0 1 0 outline-alpha])
(def ^:private neutral-color  [1 1 1 outline-alpha])

(defmethod entity/render-below! :entity/mouseover?
  [_ {:keys [entity/faction] :as entity}]
  (let [player @g/player-eid]
    (g/with-line-width 3
      #(g/draw-ellipse (:position entity)
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
                      (g/timer-ratio counter))
    (render-active-effect effect-ctx
                          ; !! FIXME !!
                          ; (update-effect-ctx effect-ctx)
                          ; - render does not need to update .. update inside active-skill
                          effects)))

(defmethod entity/render-above! :npc-sleeping
  [_ entity]
  (let [[x y] (:position entity)]
    (g/draw-text {:text "zzz"
                  :x x
                  :y (+ y (:half-height entity))
                  :up? true})))

(defmethod entity/render-below! :player-item-on-cursor
  [[_ {:keys [item]}] entity]
  (when (g/world-item?)
    (g/draw-centered (:entity/image item)
                     (g/item-place-position entity))))

(defmethod entity/render-below! :stunned
  [_ entity]
  (g/draw-circle (:position entity) 0.5 [1 1 1 0.6]))

(defmethod entity/render-above! :entity/string-effect
  [[_ {:keys [text]}] entity]
  (let [[x y] (:position entity)]
    (g/draw-text {:text text
                  :x x
                  :y (+ y
                        (:half-height entity)
                        (g/pixels->world-units 5))
                  :scale 2
                  :up? true})))

; TODO draw opacity as of counter ratio?
(defmethod entity/render-above! :entity/temp-modifier
  [_ entity]
  (g/draw-filled-circle (:position entity) 0.5 [0.5 0.5 0.5 0.4]))
