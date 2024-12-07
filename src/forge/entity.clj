(ns forge.entity
  (:require [clojure.gdx.input :refer [button-just-pressed?]]
            [clojure.gdx.math.vector2 :as v]
            [clojure.gdx.scene2d.actor :refer [visible? user-object] :as actor]
            [clojure.utils :refer [find-first defmethods]]
            [forge.animation :as animation]
            [forge.app.asset-manager :refer [play-sound]]
            [forge.app.cursors :refer [set-cursor]]
            [forge.app.db :as db]
            [forge.app.gui-viewport :refer [gui-mouse-position]]
            [forge.app.screens :refer [change-screen]]
            [forge.app.shape-drawer :as sd]
            [forge.app.vis-ui :refer [window-title-bar?
                                      button?]]
            [forge.app.world-viewport :refer [pixels->world-units world-mouse-position]]
            [forge.controls :as controls]
            [forge.effect :refer [effects-applicable?
                                  effects-useful?
                                  effects-do!
                                  effects-render]]
            [forge.entity.body :refer [e-collides?
                                       e-tile
                                       e-direction]]
            [forge.entity.inventory :refer [set-item remove-item stackable? stack-item
                                            can-pickup-item? pickup-item]]
            [forge.entity.faction :refer [e-enemy]]
            [forge.entity.fsm :refer [e-state-k
                                      send-event
                                      state-enter
                                      state-exit
                                      state-cursor]]
            [forge.entity.mana :refer [mana-value pay-mana-cost]]
            [forge.entity.modifiers :as mods]
            [forge.entity.stat :as stat]
            [forge.entity.string-effect :as string-effect]
            [forge.entity.skills :refer [has-skill?
                                         add-skill]]
            [forge.graphics :refer [draw-rotated-centered
                                    draw-image
                                    draw-centered
                                    draw-text
                                    edn->image]]
            [forge.screens.stage :refer [mouse-on-actor?]]
            [forge.screens.world :refer [e-tick
                                         render-below
                                         render-default
                                         render-above
                                         render-info
                                         draw-gui-view
                                         pause-game?
                                         manual-tick]]
            [forge.ui :refer [show-modal]]
            [forge.ui.action-bar :as action-bar]
            [forge.ui.inventory :as inventory :refer [clicked-inventory-cell valid-slot?]]
            [forge.ui.skill-window :refer [clicked-skillmenu-skill]]
            [forge.ui.player-message :as player-message]
            [forge.world :refer [minimum-body-size
                                 ->v
                                 e-create
                                 e-destroy
                                 spawn-audiovisual
                                 spawn-item
                                 delayed-alert
                                 line-of-sight?]
             :as world]
            [forge.world.grid :refer [world-grid
                                      cell-blocked?
                                      nearest-entity
                                      nearest-entity-distance
                                      rectangle->cells
                                      cells->entities
                                      circle->entities]]
            [forge.world.mouseover-entity :refer [mouseover-eid]]
            [forge.world.time :refer [world-delta
                                      stopped?
                                      timer
                                      max-delta-time
                                      finished-ratio]]
            [forge.world.player :refer [player-eid]]
            [forge.world.potential-fields :as potential-fields]
            [malli.core :as m]
            [reduce-fsm :as fsm]))

(comment
 (def ^:private entity
   {:optional [#'->v
               #'e-create
               #'e-destroy
               #'e-tick
               #'render-below
               #'render-default
               #'render-above
               #'render-info]})

 (def ^:private entity-state
   (merge-with concat
               entity
               {:optional [#'state-enter
                           #'state-exit
                           #'state-cursor
                           #'pause-game?
                           #'manual-tick
                           #'clicked-inventory-cell
                           #'clicked-skillmenu-skill
                           #'draw-gui-view]}))
 )

(defmethods :entity/temp-modifier
  (e-tick [[k {:keys [modifiers counter]}] eid]
    (when (stopped? counter)
      (swap! eid dissoc k)
      (swap! eid mods/remove modifiers)))

  ; TODO draw opacity as of counter ratio?
  (render-above [_ entity]
    (sd/filled-circle (:position entity) 0.5 [0.5 0.5 0.5 0.4])))

(defmethod e-destroy :entity/destroy-audiovisual [[_ audiovisuals-id] eid]
  (spawn-audiovisual (:position @eid)
                     (db/build audiovisuals-id)))

(def ^:private shout-radius 4)

(defn- friendlies-in-radius [position faction]
  (->> {:position position
        :radius shout-radius}
       circle->entities
       (filter #(= (:entity/faction @%) faction))))

(defmethod e-tick :entity/alert-friendlies-after-duration
  [[_ {:keys [counter faction]}] eid]
  (when (stopped? counter)
    (swap! eid assoc :entity/destroyed? true)
    (doseq [friendly-eid (friendlies-in-radius (:position @eid) faction)]
      (send-event friendly-eid :alert))))

(defmethods :entity/delete-after-duration
  (->v [[_ duration]]
    (timer duration))

  (e-tick [[_ counter] eid]
    (when (stopped? counter)
      (swap! eid assoc :entity/destroyed? true))))

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

; fsm throws when initial-state is not part of states, so no need to assert initial-state
; initial state is nil, so associng it. make bug report at reduce-fsm?
(defn- ->init-fsm [fsm initial-state]
  (assoc (fsm initial-state nil) :state initial-state))

(defmethod e-create :entity/fsm [[k {:keys [fsm initial-state]}] eid]
  (swap! eid assoc
         k (->init-fsm (case fsm
                         :fsms/player player-fsm
                         :fsms/npc npc-fsm)
                       initial-state)
         initial-state (->v [initial-state eid])))

(defmethods :entity/projectile-collision
  (->v [[_ v]]
    (assoc v :already-hit-bodies #{}))

  (e-tick [[k {:keys [entity-effects already-hit-bodies piercing?]}] eid]
    ; TODO this could be called from body on collision
    ; for non-solid
    ; means non colliding with other entities
    ; but still collding with other stuff here ? o.o
    (let [entity @eid
          cells* (map deref (rectangle->cells entity)) ; just use cached-touched -cells
          hit-entity (find-first #(and (not (contains? already-hit-bodies %)) ; not filtering out own id
                                       (not= (:entity/faction entity) ; this is not clear in the componentname & what if they dont have faction - ??
                                             (:entity/faction @%))
                                       (:collides? @%)
                                       (e-collides? entity @%))
                                 (cells->entities cells*))
          destroy? (or (and hit-entity (not piercing?))
                       (some #(cell-blocked? % (:z-order entity)) cells*))]
      (when destroy?
        (swap! eid assoc :entity/destroyed? true))
      (when hit-entity
        (swap! eid assoc-in [k :already-hit-bodies] (conj already-hit-bodies hit-entity))) ; this is only necessary in case of not piercing ...
      (when hit-entity
        (effects-do! {:effect/source eid :effect/target hit-entity} entity-effects)))))

(defn- move-position [position {:keys [direction speed delta-time]}]
  (mapv #(+ %1 (* %2 speed delta-time)) position direction))

(defn- move-body [body movement]
  (-> body
      (update :position    move-position movement)
      (update :left-bottom move-position movement)))

(defn- valid-position? [{:keys [entity/id z-order] :as body}]
  {:pre [(:collides? body)]}
  (let [cells* (into [] (map deref) (rectangle->cells body))]
    (and (not-any? #(cell-blocked? % z-order) cells*)
         (->> cells*
              cells->entities
              (not-any? (fn [other-entity]
                          (let [other-entity @other-entity]
                            (and (not= (:entity/id other-entity) id)
                                 (:collides? other-entity)
                                 (e-collides? other-entity body)))))))))

(defn- try-move [body movement]
  (let [new-body (move-body body movement)]
    (when (valid-position? new-body)
      new-body)))

; TODO sliding threshold
; TODO name - with-sliding? 'on'
; TODO if direction was [-1 0] and invalid-position then this algorithm tried to move with
; direection [0 0] which is a waste of processor power...
(defn- try-move-solid-body [body {[vx vy] :direction :as movement}]
  (let [xdir (Math/signum (float vx))
        ydir (Math/signum (float vy))]
    (or (try-move body movement)
        (try-move body (assoc movement :direction [xdir 0]))
        (try-move body (assoc movement :direction [0 ydir])))))

; set max speed so small entities are not skipped by projectiles
; could set faster than max-speed if I just do multiple smaller movement steps in one frame
(def ^:private max-speed (/ minimum-body-size max-delta-time)) ; need to make var because m/schema would fail later if divide / is inside the schema-form

(def speed-schema (m/schema [:and number? [:>= 0] [:<= max-speed]]))

(defmethod e-tick :entity/movement
  [[_ {:keys [direction speed rotate-in-movement-direction?] :as movement}]
   eid]
  (assert (m/validate speed-schema speed)
          (pr-str speed))
  (assert (or (zero? (v/length direction))
              (v/normalised? direction))
          (str "cannot understand direction: " (pr-str direction)))
  (when-not (or (zero? (v/length direction))
                (nil? speed)
                (zero? speed))
    (let [movement (assoc movement :delta-time world-delta)
          body @eid]
      (when-let [body (if (:collides? body) ; < == means this is a movement-type ... which could be a multimethod ....
                        (try-move-solid-body body movement)
                        (move-body body movement))]
        (world/entity-position-changed eid)
        (swap! eid assoc
               :position (:position body)
               :left-bottom (:left-bottom body))
        (when rotate-in-movement-direction?
          (swap! eid assoc :rotation-angle (v/angle-from-vector direction)))))))

(defmethods :entity/skills
  (e-create [[k skills] eid]
    (swap! eid assoc k nil)
    (doseq [skill skills]
      (swap! eid add-skill skill)))

  (e-tick [[k skills] eid]
    (doseq [{:keys [skill/cooling-down?] :as skill} (vals skills)
            :when (and cooling-down?
                       (stopped? cooling-down?))]
      (swap! eid assoc-in [k (:property/id skill) :skill/cooling-down?] false))))

(defmethod render-default :entity/clickable [[_ {:keys [text]}] {:keys [entity/mouseover?] :as entity}]
  (when (and mouseover? text)
    (let [[x y] (:position entity)]
      (draw-text {:text text
                  :x x
                  :y (+ y (:half-height entity))
                  :up? true}))))

(defmethod render-default :entity/line-render [[_ {:keys [thick? end color]}] entity]
  (let [position (:position entity)]
    (if thick?
      (sd/with-line-width 4
        #(sd/line position end color))
      (sd/line position end color))))

(defmethod render-default :entity/image [[_ image] entity]
  (draw-rotated-centered image
                         (or (:rotation-angle entity) 0)
                         (:position entity)))

(def ^:private outline-alpha 0.4)
(def ^:private enemy-color    [1 0 0 outline-alpha])
(def ^:private friendly-color [0 1 0 outline-alpha])
(def ^:private neutral-color  [1 1 1 outline-alpha])

(defmethod render-below :entity/mouseover? [_ {:keys [entity/faction] :as entity}]
  (let [player @player-eid]
    (sd/with-line-width 3
      #(sd/ellipse (:position entity)
                   (:half-width entity)
                   (:half-height entity)
                   (cond (= faction (e-enemy player))
                         enemy-color
                         (= faction (:entity/faction player))
                         friendly-color
                         :else
                         neutral-color)))))

(defn- assoc-image-current-frame [entity animation]
  (assoc entity :entity/image (animation/current-frame animation)))

(defmethods :entity/animation
  (e-create [[_ animation] eid]
    (swap! eid assoc-image-current-frame animation))

  (e-tick [[k animation] eid]
    (swap! eid #(-> %
                    (assoc-image-current-frame animation)
                    (assoc k (animation/tick animation world-delta))))))

(defmethods :entity/delete-after-animation-stopped
  (e-create  [_ eid]
    (-> @eid :entity/animation :looping? not assert))

  (e-tick [_ eid]
    (when (animation/stopped? (:entity/animation @eid))
      (swap! eid assoc :entity/destroyed? true))))

(defmethod ->v :npc-dead [[_ eid]]
  {:eid eid})

(defmethod state-enter :npc-dead [[_ {:keys [eid]}]]
  (swap! eid assoc :entity/destroyed? true))

(defn- nearest-enemy [entity]
  (nearest-entity @(get world-grid (e-tile entity))
                  (e-enemy entity)))

(defn- npc-effect-ctx [eid]
  (let [entity @eid
        target (nearest-enemy entity)
        target (when (and target (line-of-sight? entity @target))
                 target)]
    {:effect/source eid
     :effect/target target
     :effect/target-direction (when target (e-direction entity @target))}))

(comment
 (let [eid (ids->eids 76)
       effect-ctx (npc-effect-ctx eid)]
   (npc-choose-skill effect-ctx @eid))
 )

(defn- not-enough-mana? [entity {:keys [skill/cost]}]
  (and cost (> cost (mana-value entity))))

(defn- skill-usable-state
  [entity
   {:keys [skill/cooling-down? skill/effects] :as skill}
   effect-ctx]
  (cond
   cooling-down?
   :cooldown

   (not-enough-mana? entity skill)
   :not-enough-mana

   (not (effects-applicable? effect-ctx effects))
   :invalid-params

   :else
   :usable))

(defn- npc-choose-skill [entity ctx]
  (->> entity
       :entity/skills
       vals
       (sort-by #(or (:skill/cost %) 0))
       reverse
       (filter #(and (= :usable (skill-usable-state entity % ctx))
                     (effects-useful? ctx (:skill/effects %))))
       first))

(defmethods :npc-idle
  (->v [[_ eid]]
    {:eid eid})

  (e-tick [_ eid]
    (let [effect-ctx (npc-effect-ctx eid)]
      (if-let [skill (npc-choose-skill @eid effect-ctx)]
        (send-event eid :start-action [skill effect-ctx])
        (send-event eid :movement-direction (or (potential-fields/find-direction eid) [0 0]))))))

; npc moving is basically a performance optimization so npcs do not have to check
; usable skills every frame
; also prevents fast twitching around changing directions every frame
(defmethods :npc-moving
  (->v [[_ eid movement-vector]]
    {:eid eid
     :movement-vector movement-vector
     :counter (timer (* (stat/->value @eid :entity/reaction-time) 0.016))})

  (state-enter [[_ {:keys [eid movement-vector]}]]
    (swap! eid assoc :entity/movement {:direction movement-vector
                                       :speed (or (stat/->value @eid :entity/movement-speed) 0)}))

  (state-exit [[_ {:keys [eid]}]]
    (swap! eid dissoc :entity/movement))

  (e-tick [[_ {:keys [counter]}] eid]
    (when (stopped? counter)
      (send-event eid :timer-finished))))

(defmethods :npc-sleeping
  (->v [[_ eid]]
    {:eid eid})

  (state-exit [[_ {:keys [eid]}]]
    (delayed-alert (:position @eid) (:entity/faction @eid) 0.2)
    (swap! eid string-effect/add "[WHITE]!"))

  (e-tick [_ eid]
    (let [entity @eid
          cell (get world-grid (e-tile entity))] ; pattern!
      (when-let [distance (nearest-entity-distance @cell (e-enemy entity))]
        (when (<= distance (stat/->value entity :entity/aggro-range))
          (send-event eid :alert)))))

  (render-above [_ entity]
    (let [[x y] (:position entity)]
      (draw-text {:text "zzz"
                  :x x
                  :y (+ y (:half-height entity))
                  :up? true}))))

(defmethods :player-dead
  (state-cursor [_]
    :cursors/black-x)

  (pause-game? [_]
    true)

  (state-enter [_]
    (play-sound "bfxr_playerdeath")
    (show-modal {:title "YOU DIED"
                 :text "\nGood luck next time"
                 :button-text ":("
                 :on-click #(change-screen :screens/main-menu)})))

(defmethods :stunned
  (->v [[_ eid duration]]
    {:eid eid
     :counter (timer duration)})

  (state-cursor [_]
    :cursors/denied)

  (pause-game? [_]
    false)

  (e-tick [[_ {:keys [counter]}] eid]
    (when (stopped? counter)
      (send-event eid :effect-wears-off)))

  (render-below [_ entity]
    (sd/circle (:position entity) 0.5 [1 1 1 0.6])))

(defmethods :player-moving
  (->v [[_ eid movement-vector]]
    {:eid eid
     :movement-vector movement-vector})

  (state-cursor [_]
    :cursors/walking)

  (pause-game? [_]
    false)

  (state-enter [[_ {:keys [eid movement-vector]}]]
    (swap! eid assoc :entity/movement {:direction movement-vector
                                       :speed (stat/->value @eid :entity/movement-speed)}))

  (state-exit [[_ {:keys [eid]}]]
    (swap! eid dissoc :entity/movement))

  (e-tick [[_ {:keys [movement-vector]}] eid]
    (if-let [movement-vector (controls/movement-vector)]
      (swap! eid assoc :entity/movement {:direction movement-vector
                                         :speed (stat/->value @eid :entity/movement-speed)})
      (send-event eid :no-movement-input))))

(defn- apply-action-speed-modifier [entity skill action-time]
  (/ action-time
     (or (stat/->value entity (:skill/action-time-modifier-key skill))
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
    (sd/filled-circle center radius [1 1 1 0.125])
    (sd/sector center radius
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

  (state-cursor [_]
    :cursors/sandclock)

  (pause-game? [_]
    false)

  (state-enter [[_ {:keys [eid skill]}]]
    (play-sound (:skill/start-action-sound skill))
    (when (:skill/cooldown skill)
      (swap! eid assoc-in
             [:entity/skills (:property/id skill) :skill/cooling-down?]
             (timer (:skill/cooldown skill))))
    (when (and (:skill/cost skill)
               (not (zero? (:skill/cost skill))))
      (swap! eid pay-mana-cost (:skill/cost skill))))

  (e-tick [[_ {:keys [skill effect-ctx counter]}] eid]
    (cond
     (not (effects-applicable? (check-update-ctx effect-ctx)
                               (:skill/effects skill)))
     (do
      (send-event eid :action-done)
      ; TODO some sound ?
      )

     (stopped? counter)
     (do
      (effects-do! effect-ctx (:skill/effects skill))
      (send-event eid :action-done))))

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
     (visible? (inventory/window))
     (do
      (play-sound "bfxr_takeit")
      (swap! eid assoc :entity/destroyed? true)
      (send-event player-eid :pickup-item item))

     (can-pickup-item? @player-eid item)
     (do
      (play-sound "bfxr_pickup")
      (swap! eid assoc :entity/destroyed? true)
      (pickup-item player-eid item))

     :else
     (do
      (play-sound "bfxr_denied")
      (player-message/show "Your Inventory is full")))))

(defmethod on-clicked :clickable/player [_]
  (actor/toggle-visible! (inventory/window)))

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
    [(clickable->cursor @clicked-eid false) (fn [] (on-clicked clicked-eid))]
    [(clickable->cursor @clicked-eid true)  (fn [] (denied "Too far away"))]))

(defn- inventory-cell-with-item? [^com.badlogic.gdx.scenes.scene2d.Actor actor]
  (and (.getParent actor)
       (= "inventory-cell" (.getName (.getParent actor)))
       (get-in (:entity/inventory @player-eid)
               (user-object (.getParent actor)))))

(defn- mouseover-actor->cursor []
  (let [actor (mouse-on-actor?)]
    (cond
     (inventory-cell-with-item? actor) :cursors/hand-before-grab
     (window-title-bar? actor)      :cursors/move-window
     (button? actor)                :cursors/over-button
     :else                             :cursors/default)))

(defn- player-effect-ctx [eid]
  (let [target-position (or (and mouseover-eid (:position @mouseover-eid))
                            (world-mouse-position))]
    {:effect/source eid
     :effect/target mouseover-eid
     :effect/target-position target-position
     :effect/target-direction (v/direction (:position @eid) target-position)}))

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
             state (skill-usable-state entity skill effect-ctx)]
         (if (= state :usable)
           (do
            ; TODO cursor AS OF SKILL effect (SWORD !) / show already what the effect would do ? e.g. if it would kill highlight
            ; different color ?
            ; => e.g. meditation no TARGET .. etc.
            [:cursors/use-skill
             (fn [] (send-event eid :start-action [skill effect-ctx]))])
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

  (pause-game? [_]
    true)

  (manual-tick [[_ {:keys [eid]}]]
    (if-let [movement-vector (controls/movement-vector)]
      (send-event eid :movement-input movement-vector)
      (let [[cursor on-click] (interaction-state eid)]
        (set-cursor cursor)
        (when (button-just-pressed? :left)
          (on-click)))))

  (clicked-inventory-cell [[_ {:keys [eid]}] cell]
    ; TODO no else case
    (when-let [item (get-in (:entity/inventory @eid) cell)]
      (play-sound "bfxr_takeit")
      (send-event eid :pickup-item item)
      (remove-item eid cell)))

  (clicked-skillmenu-skill [[_ {:keys [eid]}] skill]
    (let [free-skill-points (:entity/free-skill-points @eid)]
      ; TODO no else case, no visible free-skill-points
      (when (and (pos? free-skill-points)
                 (not (has-skill? @eid skill)))
        (swap! eid assoc :entity/free-skill-points (dec free-skill-points))
        (swap! eid add-skill skill)))))

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
      (set-item eid cell item-on-cursor)
      (send-event eid :dropped-item))

     ; STACK ITEMS
     (and item-in-cell
          (stackable? item-in-cell item-on-cursor))
     (do
      (play-sound "bfxr_itemput")
      (swap! eid dissoc :entity/item-on-cursor)
      (stack-item eid cell item-on-cursor)
      (send-event eid :dropped-item))

     ; SWAP ITEMS
     (and item-in-cell
          (valid-slot? cell item-on-cursor))
     (do
      (play-sound "bfxr_itemput")
      ; need to dissoc and drop otherwise state enter does not trigger picking it up again
      ; TODO? coud handle pickup-item from item-on-cursor state also
      (swap! eid dissoc :entity/item-on-cursor)
      (remove-item eid cell)
      (set-item eid cell item-on-cursor)
      (send-event eid :dropped-item)
      (send-event eid :pickup-item item-in-cell)))))

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
                   (world-mouse-position)
                   ; so you cannot put it out of your own reach
                   (- (:entity/click-distance-tiles entity) 0.1)))

(defn- world-item? []
  (not (mouse-on-actor?)))

(defmethods :player-item-on-cursor
  (->v [[_ eid item]]
    {:eid eid
     :item item})

  (pause-game? [_]
    true)

  (manual-tick [[_ {:keys [eid]}]]
    (when (and (button-just-pressed? :left)
               (world-item?))
      (send-event eid :drop-item)))

  (clicked-inventory-cell [[_ {:keys [eid]}] cell]
    (clicked-cell eid cell))

  (state-cursor [_]
    :cursors/hand-grab)

  (state-enter [[_ {:keys [eid item]}]]
    (swap! eid assoc :entity/item-on-cursor item))

  (state-exit [[_ {:keys [eid]}]]
    ; at clicked-cell when we put it into a inventory-cell
    ; we do not want to drop it on the ground too additonally,
    ; so we dissoc it there manually. Otherwise it creates another item
    ; on the ground
    (let [entity @eid]
      (when (:entity/item-on-cursor entity)
        (play-sound "bfxr_itemputground")
        (swap! eid dissoc :entity/item-on-cursor)
        (spawn-item (item-place-position entity) (:entity/item-on-cursor entity)))))

  (draw-gui-view [[_ {:keys [eid]}]]
    (let [entity @eid]
      (when (and (= :player-item-on-cursor (e-state-k entity))
                 (not (world-item?)))
        (draw-centered (:entity/image (:entity/item-on-cursor entity))
                       (gui-mouse-position)))))

  (render-below [[_ {:keys [item]}] entity]
    (when (world-item?)
      (draw-centered (:entity/image item)
                     (item-place-position entity)))))
