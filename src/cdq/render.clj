(ns cdq.render
  (:require [cdq.context :as context]
            [cdq.db :as db]
            [cdq.effect-context :as effect-ctx]
            [cdq.entity :as entity]
            [cdq.entity.fsm :as fsm]
            [cdq.schema :as schema]
            [cdq.timer :as timer]
            cdq.error
            [cdq.grid :as grid]
            [cdq.line-of-sight :as los]
            cdq.potential-fields
            cdq.time
            [cdq.widgets.inventory :as widgets.inventory]
            [cdq.world :refer [nearest-enemy
                               player-movement-vector
                               friendlies-in-radius
                               minimum-size
                               world-item?
                               player-movement-vector
                               get-inventory
                               show-player-msg
                               selected-skill]]
            [cdq.world.potential-field :as potential-field]
            [cdq.assets :as assets]
            [cdq.audio.sound :as sound]
            [cdq.data.grid2d :as g2d]
            [cdq.graphics :as graphics]
            [cdq.graphics.animation :as animation]
            [cdq.graphics.camera :as camera]
            [cdq.graphics.shape-drawer :as sd]
            [cdq.graphics.tiled-map-renderer :as tiled-map-renderer]
            [cdq.input :as input]
            [cdq.math.raycaster :as raycaster]
            [cdq.math.vector2 :as v]
            [cdq.ui :as ui]
            [cdq.ui.actor :as actor]
            [cdq.ui.group :as group]
            [cdq.ui.stage :as stage]
            [cdq.utils :as utils :refer [find-first]])
  (:import (com.badlogic.gdx.graphics Color)
           (com.badlogic.gdx.graphics.g2d Batch)
           (cdq StageWithState)))

(defmulti manual-tick (fn [[k] context]
                        k))
(defmethod manual-tick :default [_ c])

(defn player-state-input [{:keys [cdq.context/player-eid] :as c}]
  (manual-tick (entity/state-obj @player-eid) c)
  c)

(defn- player-effect-ctx [{:keys [cdq.context/mouseover-eid
                                  cdq.graphics/world-viewport]} eid]
  (let [target-position (or (and mouseover-eid
                                 (:position @mouseover-eid))
                            (cdq.graphics/world-mouse-position world-viewport))]
    {:effect/source eid
     :effect/target mouseover-eid
     :effect/target-position target-position
     :effect/target-direction (v/direction (:position @eid) target-position)}))

(defmulti ^:private on-clicked
  (fn [eid c]
    (:type (:entity/clickable @eid))))

(defmethod on-clicked :clickable/item [eid {:keys [cdq.context/player-eid] :as c}]
  (let [item (:entity/item @eid)]
    (cond
     (actor/visible? (get-inventory c))
     (do
      (sound/play (assets/sound (:cdq/assets c) "bfxr_takeit"))
      (swap! eid assoc :entity/destroyed? true)
      (fsm/event c player-eid :pickup-item item))

     (entity/can-pickup-item? @player-eid item)
     (do
      (sound/play (assets/sound (:cdq/assets c) "bfxr_pickup"))
      (swap! eid assoc :entity/destroyed? true)
      (widgets.inventory/pickup-item c player-eid item))

     :else
     (do
      (sound/play (assets/sound (:cdq/assets c) "bfxr_denied"))
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
                                              (sound/play (assets/sound (:cdq/assets c) "bfx_denied"))
                                              (show-player-msg c "Too far away"))]))

(defn- inventory-cell-with-item? [{:keys [cdq.context/player-eid] :as c} actor]
  (and (actor/parent actor)
       (= "inventory-cell" (actor/name (actor/parent actor)))
       (get-in (:entity/inventory @player-eid)
               (actor/user-object (actor/parent actor)))))

(defn- mouseover-actor->cursor [{:keys [cdq.context/stage] :as c}]
  (let [actor (stage/mouse-on-actor? stage)]
    (cond
     (inventory-cell-with-item? c actor) :cursors/hand-before-grab
     (ui/window-title-bar? actor)           :cursors/move-window
     (ui/button? actor)                     :cursors/over-button
     :else                               :cursors/default)))

(defn- not-enough-mana? [entity {:keys [skill/cost]}]
  (and cost (> cost (entity/mana-val entity))))

(defn- skill-usable-state
  [entity {:keys [skill/cooling-down? skill/effects] :as skill} effect-ctx]
  (cond
   cooling-down?
   :cooldown

   (not-enough-mana? entity skill)
   :not-enough-mana

   (not (effect-ctx/some-applicable? effect-ctx effects))
   :invalid-params

   :else
   :usable))

(defn- interaction-state [{:keys [cdq.context/mouseover-eid
                                  cdq.context/stage] :as c} eid]
  (let [entity @eid]
    (cond
     (stage/mouse-on-actor? stage)
     [(mouseover-actor->cursor c)
      (fn [] nil)] ; handled by actors themself, they check player state

     (and mouseover-eid
          (:entity/clickable @mouseover-eid))
     (clickable-entity-interaction c entity mouseover-eid)

     :else
     (if-let [skill-id (selected-skill c)]
       (let [skill (skill-id (:entity/skills entity))
             effect-ctx (player-effect-ctx c eid)
             state (skill-usable-state entity skill effect-ctx)]
         (if (= state :usable)
           (do
            ; TODO cursor AS OF SKILL effect (SWORD !) / show already what the effect would do ? e.g. if it would kill highlight
            ; different color ?
            ; => e.g. meditation no TARGET .. etc.
            [:cursors/use-skill
             (fn []
               (fsm/event c eid :start-action [skill effect-ctx]))])
           (do
            ; TODO cursor as of usable state
            ; cooldown -> sanduhr kleine
            ; not-enough-mana x mit kreis?
            ; invalid-params -> depends on params ...
            [:cursors/skill-not-usable
             (fn []
               (sound/play (assets/sound (:cdq/assets c) "bfxr_denied"))
               (show-player-msg c (case state
                                    :cooldown "Skill is still on cooldown"
                                    :not-enough-mana "Not enough mana"
                                    :invalid-params "Cannot use this here")))])))
       [:cursors/no-skill-selected
        (fn []
          (sound/play (assets/sound (:cdq/assets c) "bfxr_denied"))
          (show-player-msg c "No selected skill"))]))))

(defmethod manual-tick :player-idle [[_ {:keys [eid]}] c]
  (if-let [movement-vector (player-movement-vector)]
    (fsm/event c eid :movement-input movement-vector)
    (let [[cursor on-click] (interaction-state c eid)]
      (cdq.graphics/set-cursor c cursor)
      (when (input/button-just-pressed? :left)
        (on-click)))))

(defmethod manual-tick :player-item-on-cursor [[_ {:keys [eid]}] c]
  (when (and (input/button-just-pressed? :left)
             (world-item? c))
    (fsm/event c eid :drop-item)))

(defn- active-entities [{:keys [grid]} center-entity]
  (->> (let [idx (-> center-entity
                     :cdq.content-grid/content-cell
                     deref
                     :idx)]
         (cons idx (g2d/get-8-neighbour-positions idx)))
       (keep grid)
       (mapcat (comp :entities deref))))

(defn assoc-active-entities [{:keys [cdq.context/content-grid
                                      cdq.context/player-eid]
                               :as context}]
  (assoc context :cdq.game/active-entities (active-entities content-grid @player-eid)))

(defn set-camera-on-player
  [{:keys [cdq.graphics/world-viewport
           cdq.context/player-eid]
    :as context}]
  {:pre [world-viewport
         player-eid]}
  (camera/set-position (:camera world-viewport)
                       (:position @player-eid))
  context)

(defn clear-screen! [context]
  (com.badlogic.gdx.utils.ScreenUtils/clear com.badlogic.gdx.graphics.Color/BLACK)
  context)

(def ^:private explored-tile-color (Color. (float 0.5) (float 0.5) (float 0.5) (float 1)))

(def ^:private ^:dbg-flag see-all-tiles? false)

(comment
 (def ^:private count-rays? false)

 (def ray-positions (atom []))
 (def do-once (atom true))

 (count @ray-positions)
 2256
 (count (distinct @ray-positions))
 608
 (* 608 4)
 2432
 )

(defn- tile-color-setter [raycaster explored-tile-corners light-position]
  #_(reset! do-once false)
  (let [light-cache (atom {})]
    (fn tile-color-setter [_color x y]
      (let [position [(int x) (int y)]
            explored? (get @explored-tile-corners position) ; TODO needs int call ?
            base-color (if explored? explored-tile-color Color/BLACK)
            cache-entry (get @light-cache position :not-found)
            blocked? (if (= cache-entry :not-found)
                       (let [blocked? (raycaster/blocked? raycaster light-position position)]
                         (swap! light-cache assoc position blocked?)
                         blocked?)
                       cache-entry)]
        #_(when @do-once
            (swap! ray-positions conj position))
        (if blocked?
          (if see-all-tiles? Color/WHITE base-color)
          (do (when-not explored?
                (swap! explored-tile-corners assoc (mapv int position) true))
              Color/WHITE))))))

(defn render-tiled-map! [{:keys [cdq.graphics/world-viewport
                                 cdq.context/tiled-map
                                 cdq.context/raycaster
                                 cdq.context/explored-tile-corners]
                          :as context}]
  (tiled-map-renderer/draw context
                           tiled-map
                           (tile-color-setter raycaster
                                              explored-tile-corners
                                              (camera/position (:camera world-viewport))))
  context)

(def ^:private render-fns
  '[(cdq.render.draw-on-world-view.before-entities/render)
    (cdq.render.draw-on-world-view.entities/render-entities
     {:below {:entity/mouseover? cdq.render.draw-on-world-view.entities/draw-faction-ellipse
              :player-item-on-cursor cdq.render.draw-on-world-view.entities/draw-world-item-if-exists
              :stunned cdq.render.draw-on-world-view.entities/draw-stunned-circle}
      :default {:entity/image cdq.render.draw-on-world-view.entities/draw-image-as-of-body
                :entity/clickable cdq.render.draw-on-world-view.entities/draw-text-when-mouseover-and-text
                :entity/line-render cdq.render.draw-on-world-view.entities/draw-line}
      :above {:npc-sleeping cdq.render.draw-on-world-view.entities/draw-zzzz
              :entity/string-effect cdq.render.draw-on-world-view.entities/draw-text
              :entity/temp-modifier cdq.render.draw-on-world-view.entities/draw-filled-circle-grey}
      :info {:entity/hp cdq.render.draw-on-world-view.entities/draw-hpbar-when-mouseover-and-not-full
             :active-skill cdq.render.draw-on-world-view.entities/draw-skill-image-and-active-effect}})
    (cdq.render.draw-on-world-view.after-entities/render)])

(defn- draw-with [{:keys [^Batch cdq.graphics/batch
                          cdq.graphics/shape-drawer] :as c}
                 viewport
                 unit-scale
                 draw-fn]
  (.setColor batch Color/WHITE) ; fix scene2d.ui.tooltip flickering
  (.setProjectionMatrix batch (camera/combined (:camera viewport)))
  (.begin batch)
  (sd/with-line-width shape-drawer unit-scale
    (fn []
      (draw-fn (assoc c :cdq.context/unit-scale unit-scale))))
  (.end batch))

(defn- draw-on-world-view* [{:keys [cdq.graphics/world-unit-scale
                                    cdq.graphics/world-viewport] :as c} render-fn]
  (draw-with c
             world-viewport
             world-unit-scale
             render-fn))

(defn draw-on-world-view! [context]
  (draw-on-world-view* context
                       (fn [context]
                         (doseq [f render-fns]
                           (utils/req-resolve-call f context))))
  context)

(defn render-stage! [{:keys [^StageWithState cdq.context/stage] :as context}]
  (set! (.applicationState stage) (assoc context :cdq.context/unit-scale 1))
  (com.badlogic.gdx.scenes.scene2d.Stage/.draw stage)
  (set! (.applicationState stage) context)
  (com.badlogic.gdx.scenes.scene2d.Stage/.act stage)
  context)

(defn update-mouseover-entity! [{:keys [cdq.context/grid
                                        cdq.context/mouseover-eid
                                        cdq.context/player-eid
                                        cdq.graphics/world-viewport
                                        cdq.context/stage] :as c}]
  (let [new-eid (if (stage/mouse-on-actor? stage)
                  nil
                  (let [player @player-eid
                        hits (remove #(= (:z-order @%) :z-order/effect)
                                     (cdq.grid/point->entities grid (cdq.graphics/world-mouse-position world-viewport)))]
                    (->> cdq.world/render-z-order
                         (cdq.utils/sort-by-order hits #(:z-order @%))
                         reverse
                         (filter #(los/exists? c player @%))
                         first)))]
    (when mouseover-eid
      (swap! mouseover-eid dissoc :entity/mouseover?))
    (when new-eid
      (swap! new-eid assoc :entity/mouseover? true))
    (assoc c :cdq.context/mouseover-eid new-eid)))

(defn update-paused! [{:keys [cdq.context/player-eid
                              context/entity-components
                              error ; FIXME ! not `::` keys so broken !
                              ] :as c}]
  (let [pausing? true]
    (assoc c :cdq.context/paused? (or error
                                      (and pausing?
                                           (get-in entity-components [(cdq.entity/state-k @player-eid) :pause-game?])
                                           (not (or (input/key-just-pressed? :p)
                                                    (input/key-pressed?      :space))))))))

(defn- update-time [context]
  (let [delta-ms (min (graphics/delta-time)
                      cdq.time/max-delta)]
    (-> context
        (update :cdq.context/elapsed-time + delta-ms)
        (assoc :cdq.context/delta-time delta-ms))))

(defn- update-potential-fields! [{:keys [cdq.context/factions-iterations
                                         cdq.context/grid
                                         world/potential-field-cache
                                         cdq.game/active-entities]
                                  :as context}]
  (doseq [[faction max-iterations] factions-iterations]
    (cdq.potential-fields/tick potential-field-cache
                               grid
                               faction
                               active-entities
                               max-iterations))
  context)

(defmulti tick! (fn [[k] eid c]
                  k))
(defmethod tick! :default [_ eid c])

(defn- tick-entities! [{:keys [cdq.game/active-entities]
                        :as context}]
  ; precaution in case a component gets removed by another component
  ; the question is do we still want to update nil components ?
  ; should be contains? check ?
  ; but then the 'order' is important? in such case dependent components
  ; should be moved together?
  (try
   (doseq [eid active-entities]
     (try
      (doseq [k (keys @eid)]
        (try (when-let [v (k @eid)]
               (tick! [k v] eid context))
             (catch Throwable t
               (throw (ex-info "entity-tick" {:k k} t)))))
      (catch Throwable t
        (throw (ex-info "" (select-keys @eid [:entity/id]) t)))))
   (catch Throwable t
     (cdq.error/error-window context t)
     #_(bind-root ::error t))) ; FIXME ... either reduce or use an atom ...
  context)

; this is not necessary if effect does not need target, but so far not other solution came up.
(defn- update-effect-ctx
  "Call this on effect-context if the time of using the context is not the time when context was built."
  [context {:keys [effect/source effect/target] :as effect-ctx}]
  (if (and target
           (not (:entity/destroyed? @target))
           (los/exists? context @source @target))
    effect-ctx
    (dissoc effect-ctx :effect/target)))

(defmethod tick! :active-skill [[_ {:keys [skill effect-ctx counter]}]
                                eid
                                {:keys [cdq.context/elapsed-time] :as c}]
  (cond
   (not (effect-ctx/some-applicable? (update-effect-ctx c effect-ctx)
                                     (:skill/effects skill)))
   (do
    (fsm/event c eid :action-done)
    ; TODO some sound ?
    )

   (timer/stopped? counter elapsed-time)
   (do
    (effect-ctx/do-all! c effect-ctx (:skill/effects skill))
    (fsm/event c eid :action-done))))

(defn- npc-choose-skill [c entity ctx]
  (->> entity
       :entity/skills
       vals
       (sort-by #(or (:skill/cost %) 0))
       reverse
       (filter #(and (= :usable (skill-usable-state entity % ctx))
                     (effect-ctx/applicable-and-useful? c ctx (:skill/effects %))))
       first))

(defn- npc-effect-context [c eid]
  (let [entity @eid
        target (nearest-enemy c entity)
        target (when (and target
                          (los/exists? c entity @target))
                 target)]
    {:effect/source eid
     :effect/target target
     :effect/target-direction (when target
                                (entity/direction entity @target))}))

(defmethod tick! :npc-idle [_ eid c]
  (let [effect-ctx (npc-effect-context c eid)]
    (if-let [skill (npc-choose-skill c @eid effect-ctx)]
      (fsm/event c eid :start-action [skill effect-ctx])
      (fsm/event c eid :movement-direction (or (potential-field/find-direction c eid) [0 0])))))

(defmethod tick! :npc-moving [[_ {:keys [counter]}]
                              eid
                              {:keys [cdq.context/elapsed-time] :as c}]
  (when (timer/stopped? counter elapsed-time)
    (fsm/event c eid :timer-finished)))

(defmethod tick! :npc-sleeping [_ eid {:keys [cdq.context/grid] :as c}]
  (let [entity @eid
        cell (grid (entity/tile entity))]
    (when-let [distance (grid/nearest-entity-distance @cell (entity/enemy entity))]
      (when (<= distance (entity/stat entity :entity/aggro-range))
        (fsm/event c eid :alert)))))

(defmethod tick! :player-moving [[_ {:keys [movement-vector]}] eid c]
  (if-let [movement-vector (player-movement-vector)]
    (swap! eid assoc :entity/movement {:direction movement-vector
                                       :speed (entity/stat @eid :entity/movement-speed)})
    (fsm/event c eid :no-movement-input)))

(defmethod tick! :stunned [[_ {:keys [counter]}]
                           eid
                           {:keys [cdq.context/elapsed-time] :as c}]
  (when (timer/stopped? counter elapsed-time)
    (fsm/event c eid :effect-wears-off)))

(defmethod tick! :entity/alert-friendlies-after-duration
  [[_ {:keys [counter faction]}]
   eid
   {:keys [cdq.context/grid
           cdq.context/elapsed-time]
    :as c}]
  (when (timer/stopped? counter elapsed-time)
    (swap! eid assoc :entity/destroyed? true)
    (doseq [friendly-eid (friendlies-in-radius grid (:position @eid) faction)]
      (fsm/event c friendly-eid :alert))))

(defmethod tick! :entity/animation
  [[k animation] eid {:keys [cdq.context/delta-time]}]
  (swap! eid #(-> %
                  (assoc :entity/image (animation/current-frame animation))
                  (assoc k (animation/tick animation delta-time)))))

(defmethod tick! :entity/delete-after-duration
  [[_ counter]
   eid
   {:keys [cdq.context/elapsed-time]}]
  (when (timer/stopped? counter elapsed-time)
    (swap! eid assoc :entity/destroyed? true)))

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
(def ^:private max-speed (/ minimum-size
                            cdq.time/max-delta)) ; need to make var because s/schema would fail later if divide / is inside the schema-form

(def speed-schema (schema/m-schema [:and number? [:>= 0] [:<= max-speed]]))

(defmethod tick! :entity/movement
  [[_ {:keys [direction speed rotate-in-movement-direction?] :as movement}]
   eid
   {:keys [cdq.context/delta-time
           cdq.context/grid] :as context}]
  (assert (schema/validate speed-schema speed)
          (pr-str speed))
  (assert (or (zero? (v/length direction))
              (v/normalised? direction))
          (str "cannot understand direction: " (pr-str direction)))
  (when-not (or (zero? (v/length direction))
                (nil? speed)
                (zero? speed))
    (let [movement (assoc movement :delta-time delta-time)
          body @eid]
      (when-let [body (if (:collides? body) ; < == means this is a movement-type ... which could be a multimethod ....
                        (try-move-solid-body grid body movement)
                        (move-body body movement))]
        (doseq [component context]
          (context/position-changed component eid))
        (swap! eid assoc
               :position (:position body)
               :left-bottom (:left-bottom body))
        (when rotate-in-movement-direction?
          (swap! eid assoc :rotation-angle (v/angle-from-vector direction)))))))

(defmethod tick! :entity/projectile-collision
  [[k {:keys [entity-effects already-hit-bodies piercing?]}]
   eid
   {:keys [cdq.context/grid] :as c}]
  ; TODO this could be called from body on collision
  ; for non-solid
  ; means non colliding with other entities
  ; but still collding with other stuff here ? o.o
  (let [entity @eid
        cells* (map deref (grid/rectangle->cells grid entity)) ; just use cached-touched -cells
        hit-entity (find-first #(and (not (contains? already-hit-bodies %)) ; not filtering out own id
                                     (not= (:entity/faction entity) ; this is not clear in the componentname & what if they dont have faction - ??
                                           (:entity/faction @%))
                                     (:collides? @%)
                                     (entity/collides? entity @%))
                               (grid/cells->entities cells*))
        destroy? (or (and hit-entity (not piercing?))
                     (some #(grid/blocked? % (:z-order entity)) cells*))]
    (when destroy?
      (swap! eid assoc :entity/destroyed? true))
    (when hit-entity
      (swap! eid assoc-in [k :already-hit-bodies] (conj already-hit-bodies hit-entity))) ; this is only necessary in case of not piercing ...
    (when hit-entity
      (effect-ctx/do-all! c
                          {:effect/source eid
                           :effect/target hit-entity}
                          entity-effects))))

(defmethod tick! :entity/delete-after-animation-stopped?
  [_ eid c]
  (when (animation/stopped? (:entity/animation @eid))
    (swap! eid assoc :entity/destroyed? true)))

(defmethod tick! :entity/skills
  [[k skills]
   eid
   {:keys [cdq.context/elapsed-time]}]
  (doseq [{:keys [skill/cooling-down?] :as skill} (vals skills)
          :when (and cooling-down?
                     (timer/stopped? cooling-down? elapsed-time))]
    (swap! eid assoc-in [k (:property/id skill) :skill/cooling-down?] false)))

(defmethod tick! :entity/string-effect
  [[k {:keys [counter]}]
   eid
   {:keys [cdq.context/elapsed-time]}]
  (when (timer/stopped? counter elapsed-time)
    (swap! eid dissoc k)))

(defmethod tick! :entity/temp-modifier
  [[k {:keys [modifiers counter]}]
   eid
   {:keys [cdq.context/elapsed-time]}]
  (when (timer/stopped? counter elapsed-time)
    (swap! eid dissoc k)
    (swap! eid entity/mod-remove modifiers)))

(defn when-not-paused! [context]
  (if (:cdq.context/paused? context)
    context
    (reduce (fn [context f]
              (f context))
            context
            [update-time
             update-potential-fields!
             tick-entities!])))

(defn remove-destroyed-entities! [{:keys [cdq.context/entity-ids
                                          context/entity-components]
                                   :as context}]
  (doseq [eid (filter (comp :entity/destroyed? deref)
                      (vals @entity-ids))]
    (doseq [component context]
      (context/remove-entity component eid))
    (doseq [[k v] @eid
            :let [destroy! (get-in entity-components [k :destroy!])]
            :when destroy!]
      (destroy! v eid context)))
  context)

(defn camera-controls! [{:keys [cdq.graphics/world-viewport]
                         :as context}]
  (let [camera (:camera world-viewport)
        zoom-speed 0.025]
    (when (input/key-pressed? :minus)  (camera/inc-zoom camera    zoom-speed))
    (when (input/key-pressed? :equals) (camera/inc-zoom camera (- zoom-speed))))
  context)

(defn window-controls! [{:keys [cdq.context/stage]
                         :as context}]
  (let [window-hotkeys {:inventory-window   :i
                        :entity-info-window :e}]
    (doseq [window-id [:inventory-window
                       :entity-info-window]
            :when (input/key-just-pressed? (get window-hotkeys window-id))]
      (actor/toggle-visible! (get (:windows stage) window-id))))
  (when (input/key-just-pressed? :escape)
    (let [windows (group/children (:windows stage))]
      (when (some actor/visible? windows)
        (run! #(actor/set-visible % false) windows))))
  context)
