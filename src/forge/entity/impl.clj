(ns forge.entity.impl
  (:require [forge.controls :as controls]
            [forge.entity.components :as entity :refer [hitpoints enemy add-skill collides? remove-mods event]]
            [forge.entity.state :as state]
            [forge.item :as item :refer [valid-slot? stackable?]]
            [forge.ui :as ui]
            [forge.ui.action-bar :as action-bar]
            [forge.ui.inventory :as inventory]
            [forge.world :as world :refer [audiovisual timer stopped? player-eid line-of-sight? finished-ratio mouseover-eid]]
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
               {:optional [#'state/enter
                           #'state/exit
                           #'state/cursor
                           #'state/pause-game?
                           #'state/manual-tick
                           #'state/clicked-inventory-cell
                           #'state/clicked-skillmenu-skill
                           #'state/draw-gui-view]}))
 )


(defn- applies-modifiers? [[slot _]]
  (not= :inventory.slot/bag slot))

(defn set-item [eid cell item]
  (let [entity @eid
        inventory (:entity/inventory entity)]
    (assert (and (nil? (get-in inventory cell))
                 (item/valid-slot? cell item)))
    (when (:entity/player? entity)
      (inventory/set-item-image-in-widget cell item))
    (swap! eid assoc-in (cons :entity/inventory cell) item)
    (when (applies-modifiers? cell)
      (swap! eid entity/add-mods (:entity/modifiers item)))))

(defn remove-item [eid cell]
  (let [entity @eid
        item (get-in (:entity/inventory entity) cell)]
    (assert item)
    (when (:entity/player? entity)
      (inventory/remove-item-from-widget cell))
    (swap! eid assoc-in (cons :entity/inventory cell) nil)
    (when (applies-modifiers? cell)
      (swap! eid entity/remove-mods (:entity/modifiers item)))))

; TODO doesnt exist, stackable, usable items with action/skillbar thingy
#_(defn remove-one-item [eid cell]
  (let [item (get-in (:entity/inventory @eid) cell)]
    (if (and (:count item)
             (> (:count item) 1))
      (do
       ; TODO this doesnt make sense with modifiers ! (triggered 2 times if available)
       ; first remove and then place, just update directly  item ...
       (remove-item! eid cell)
       (set-item! eid cell (update item :count dec)))
      (remove-item! eid cell))))

; TODO no items which stack are available
(defn stack-item [eid cell item]
  (let [cell-item (get-in (:entity/inventory @eid) cell)]
    (assert (item/stackable? item cell-item))
    ; TODO this doesnt make sense with modifiers ! (triggered 2 times if available)
    ; first remove and then place, just update directly  item ...
    (concat (remove-item eid cell)
            (set-item eid cell (update cell-item :count + (:count item))))))

(defn- free-cell [inventory slot item]
  (find-first (fn [[_cell cell-item]]
                (or (item/stackable? item cell-item)
                    (nil? cell-item)))
              (item/cells-and-items inventory slot)))

(defn can-pickup-item? [{:keys [entity/inventory]} item]
  (or
   (free-cell inventory (:item/slot item)   item)
   (free-cell inventory :inventory.slot/bag item)))

(defn pickup-item [eid item]
  (let [[cell cell-item] (can-pickup-item? @eid item)]
    (assert cell)
    (assert (or (item/stackable? item cell-item)
                (nil? cell-item)))
    (if (item/stackable? item cell-item)
      (stack-item eid cell item)
      (set-item   eid cell item))))

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

(defn- draw-hpbar [{:keys [position width half-width half-height]}
                   ratio]
  (let [[x y] position]
    (let [x (- x half-width)
          y (+ y half-height)
          height (pixels->world-units 5)
          border (pixels->world-units borders-px)]
      (draw-filled-rectangle x y width height black)
      (draw-filled-rectangle (+ x border)
                             (+ y border)
                             (- (* width ratio) (* 2 border))
                             (- height (* 2 border))
                             (hpbar-color ratio)))))

(defmethods :entity/hp
  (->v [[_ v]]
    [v v])

  (render-info [_ entity]
    (let [ratio (val-max-ratio (hitpoints entity))]
      (when (or (< ratio 1) (:entity/mouseover? entity))
        (draw-hpbar entity ratio)))))

(defmethod ->v :entity/mana [[_ v]] [v v])

(defmethods :entity/temp-modifier
  (e-tick [[k {:keys [modifiers counter]}] eid]
    (when (stopped? counter)
      (swap! eid dissoc k)
      (swap! eid remove-mods modifiers)))

  ; TODO draw opacity as of counter ratio?
  (render-above [_ entity]
    (draw-filled-circle (:position entity) 0.5 [0.5 0.5 0.5 0.4])))

(defmethods :entity/string-effect
  (e-tick [[k {:keys [counter]}] eid]
    (when (stopped? counter)
      (swap! eid dissoc k)))

  (render-above [[_ {:keys [text]}] entity]
    (let [[x y] (:position entity)]
      (draw-text {:text text
                  :x x
                  :y (+ y (:half-height entity) (pixels->world-units 5))
                  :scale 2
                  :up? true}))))

(defmethod e-destroy :entity/destroy-audiovisual [[_ audiovisuals-id] eid]
  (audiovisual (:position @eid) (build audiovisuals-id)))

(def ^:private shout-radius 4)

(defn- friendlies-in-radius [position faction]
  (->> {:position position
        :radius shout-radius}
       world/circle->entities
       (filter #(= (:entity/faction @%) faction))))

(defmethod e-tick :entity/alert-friendlies-after-duration
  [[_ {:keys [counter faction]}] eid]
  (when (stopped? counter)
    (swap! eid assoc :entity/destroyed? true)
    (doseq [friendly-eid (friendlies-in-radius (:position @eid) faction)]
      (event friendly-eid :alert))))

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
          cells* (map deref (world/rectangle->cells entity)) ; just use cached-touched -cells
          hit-entity (find-first #(and (not (contains? already-hit-bodies %)) ; not filtering out own id
                                       (not= (:entity/faction entity) ; this is not clear in the componentname & what if they dont have faction - ??
                                             (:entity/faction @%))
                                       (:collides? @%)
                                       (collides? entity @%))
                                 (world/cells->entities cells*))
          destroy? (or (and hit-entity (not piercing?))
                       (some #(world/blocked? % (:z-order entity)) cells*))]
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
  (let [cells* (into [] (map deref) (world/rectangle->cells body))]
    (and (not-any? #(world/blocked? % z-order) cells*)
         (->> cells*
              world/cells->entities
              (not-any? (fn [other-entity]
                          (let [other-entity @other-entity]
                            (and (not= (:entity/id other-entity) id)
                                 (:collides? other-entity)
                                 (collides? other-entity body)))))))))

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

(defmethod e-tick :entity/movement
  [[_ {:keys [direction speed rotate-in-movement-direction?] :as movement}]
   eid]
  (assert (m/validate world/speed-schema speed)
          (pr-str speed))
  (assert (or (zero? (v-length direction))
              (v-normalised? direction))
          (str "cannot understand direction: " (pr-str direction)))
  (when-not (or (zero? (v-length direction))
                (nil? speed)
                (zero? speed))
    (let [movement (assoc movement :delta-time world/delta)
          body @eid]
      (when-let [body (if (:collides? body) ; < == means this is a movement-type ... which could be a multimethod ....
                        (try-move-solid-body body movement)
                        (move-body body movement))]
        (world/position-changed eid)
        (swap! eid assoc
               :position (:position body)
               :left-bottom (:left-bottom body))
        (when rotate-in-movement-direction?
          (swap! eid assoc :rotation-angle (v-angle-from-vector direction)))))))

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

(defmethod e-create :entity/inventory [[k items] eid]
  (swap! eid assoc k item/empty-inventory)
  (doseq [item items]
    (pickup-item eid item)))

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
      (with-line-width 4
        #(draw-line position end color))
      (draw-line position end color))))

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
    (with-line-width 3
      #(draw-ellipse (:position entity)
                     (:half-width entity)
                     (:half-height entity)
                     (cond (= faction (enemy player))
                           enemy-color
                           (= faction (:entity/faction player))
                           friendly-color
                           :else
                           neutral-color)))))

(defprotocol Animation
  (animation-tick [_ delta])
  (animation-restart [_])
  (animation-stopped? [_])
  (current-frame [_]))

(defrecord ImmutableAnimation [frames frame-duration looping? cnt maxcnt]
  Animation
  (animation-tick [this delta]
    (let [maxcnt (float maxcnt)
          newcnt (+ (float cnt) (float delta))]
      (assoc this :cnt (cond (< newcnt maxcnt) newcnt
                             looping? (min maxcnt (- newcnt maxcnt))
                             :else maxcnt))))

  (animation-restart [this]
    (assoc this :cnt 0))

  (animation-stopped? [_]
    (and (not looping?) (>= cnt maxcnt)))

  (current-frame [this]
    (frames (min (int (/ (float cnt) (float frame-duration)))
                 (dec (count frames))))))

(defn- ->animation [frames & {:keys [frame-duration looping?]}]
  (map->ImmutableAnimation
   {:frames (vec frames)
    :frame-duration frame-duration
    :looping? looping?
    :cnt 0
    :maxcnt (* (count frames) (float frame-duration))}))

(defmethod edn->value :s/animation [_ {:keys [frames frame-duration looping?]}]
  (->animation (map edn->image frames)
               :frame-duration frame-duration
               :looping? looping?))

(defn- assoc-image-current-frame [entity animation]
  (assoc entity :entity/image (current-frame animation)))

(defmethods :entity/animation
  (e-create [[_ animation] eid]
    (swap! eid assoc-image-current-frame animation))

  (e-tick [[k animation] eid]
    (swap! eid #(-> %
                    (assoc-image-current-frame animation)
                    (assoc k (animation-tick animation world/delta))))))

(defmethods :entity/delete-after-animation-stopped
  (e-create  [_ eid]
    (-> @eid :entity/animation :looping? not assert))

  (e-tick [_ eid]
    (when (animation-stopped? (:entity/animation @eid))
      (swap! eid assoc :entity/destroyed? true))))

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

  (e-tick [_ eid]
    (let [effect-ctx (npc-effect-ctx eid)]
      (if-let [skill (npc-choose-skill @eid effect-ctx)]
        (entity/event eid :start-action [skill effect-ctx])
        (entity/event eid :movement-direction (or (potential-fields/find-direction eid) [0 0]))))))

; npc moving is basically a performance optimization so npcs do not have to check
; usable skills every frame
; also prevents fast twitching around changing directions every frame
(defmethods :npc-moving
  (->v [[_ eid movement-vector]]
    {:eid eid
     :movement-vector movement-vector
     :counter (timer (* (entity/stat @eid :entity/reaction-time) 0.016))})

  (state/enter [[_ {:keys [eid movement-vector]}]]
    (swap! eid assoc :entity/movement {:direction movement-vector
                                       :speed (or (entity/stat @eid :entity/movement-speed) 0)}))

  (state/exit [[_ {:keys [eid]}]]
    (swap! eid dissoc :entity/movement))

  (e-tick [[_ {:keys [counter]}] eid]
    (when (stopped? counter)
      (entity/event eid :timer-finished))))

(defmethods :npc-sleeping
  (->v [[_ eid]]
    {:eid eid})

  (state/exit [[_ {:keys [eid]}]]
    (world/shout (:position @eid) (:entity/faction @eid) 0.2)
    (swap! eid entity/add-text-effect "[WHITE]!"))

  (e-tick [_ eid]
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
    (show-modal {:title "YOU DIED"
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

  (e-tick [[_ {:keys [counter]}] eid]
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

  (e-tick [[_ {:keys [movement-vector]}] eid]
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

  (e-tick [[_ {:keys [skill effect-ctx counter]}] eid]
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
  (player-message-show text))

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
      (entity/event player-eid :pickup-item item))

     (can-pickup-item? @player-eid item)
     (do
      (play-sound "bfxr_pickup")
      (swap! eid assoc :entity/destroyed? true)
      (pickup-item player-eid item))

     :else
     (do
      (play-sound "bfxr_denied")
      (player-message-show "Your Inventory is full")))))

(defmethod on-clicked :clickable/player [_]
  (ui/toggle-visible! (inventory/window)))

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
      (remove-item eid cell)))

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
      (set-item eid cell item-on-cursor)
      (entity/event eid :dropped-item))

     ; STACK ITEMS
     (and item-in-cell
          (stackable? item-in-cell item-on-cursor))
     (do
      (play-sound "bfxr_itemput")
      (swap! eid dissoc :entity/item-on-cursor)
      (stack-item eid cell item-on-cursor)
      (entity/event eid :dropped-item))

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
