(ns forge.world.update
  (:require [anvil.db :as db]
            [anvil.controls :as controls]
            [anvil.effect :as effect]
            [anvil.entity :as entity :refer [player-eid mouseover-entity mouseover-eid line-of-sight? render-z-order projectile-size]]
            [anvil.entity.animation :as animation]
            [anvil.entity.body :as body]
            [anvil.entity.faction :as faction]
            [anvil.entity.fsm :as fsm]
            [anvil.entity.inventory :as inventory]
            [anvil.entity.modifiers :as mods]
            [anvil.entity.stat :as stat]
            [anvil.entity.skills :as skills]
            [anvil.error :as error]
            [anvil.graphics :as g]
            [anvil.input :refer [button-just-pressed?]]
            [anvil.item-on-cursor :refer [world-item?]]
            [anvil.math.vector :as v]
            [anvil.skill :as skill]
            [anvil.stage :as stage]
            [anvil.level :as level :refer [explored-tile-corners]]
            [anvil.ui :refer [window-title-bar? button?]]
            [anvil.utils :refer [defsystem]]
            [anvil.ui.actor :as actor]
            [anvil.utils :refer [bind-root sort-by-order find-first]]
            [anvil.world.grid :as grid]
            [anvil.world.time :as time :refer [stopped?]]
            [anvil.world.raycaster :refer [path-blocked?]]
            [anvil.world.potential-field :as potential-field]
            [forge.world.potential-fields :refer [update-potential-fields!]]
            [gdl.assets :refer [play-sound]]
            [malli.core :as m]))

(defsystem useful?)
(defmethod useful? :default [_ _ctx] true)

(defmethod useful? :effects.target/audiovisual [_ _]
  false)

; TODO valid params direction has to be  non-nil (entities not los player ) ?
(defmethod useful? :effects/projectile
  [[_ {:keys [projectile/max-range] :as projectile}]
   {:keys [effect/source effect/target]}]
  (let [source-p (:position @source)
        target-p (:position @target)]
    ; is path blocked ereally needed? we need LOS also right to have a target-direction as AI?
    (and (not (path-blocked? ; TODO test
                             source-p
                             target-p
                             (projectile-size projectile)))
         ; TODO not taking into account body sizes
         (< (v/distance source-p ; entity/distance function protocol EntityPosition
                        target-p)
            max-range))))

(defmethod useful? :effects/target-all [_ _]
  ; TODO
  false)

(defmethod useful? :effects/target-entity
  [[_ {:keys [maxrange]}] {:keys [effect/source effect/target]}]
  (effect/in-range? @source @target maxrange))

(defn- some-useful-and-applicable? [ctx effects]
  (->> effects
       (effect/filter-applicable? ctx)
       (some #(useful? % ctx))))

(defn- npc-choose-skill [entity ctx]
  (->> entity
       :entity/skills
       vals
       (sort-by #(or (:skill/cost %) 0))
       reverse
       (filter #(and (= :usable (skill/usable-state entity % ctx))
                     (some-useful-and-applicable? ctx (:skill/effects %))))
       first))

(defn- nearest-enemy [entity]
  (grid/nearest-entity @(grid/get (body/tile entity))
                       (faction/enemy entity)))

(defn- npc-effect-ctx [eid]
  (let [entity @eid
        target (nearest-enemy entity)
        target (when (and target
                          (line-of-sight? entity @target))
                 target)]
    {:effect/source eid
     :effect/target target
     :effect/target-direction (when target
                                (body/direction entity @target))}))


(defn- denied [text]
  (play-sound "bfxr_denied")
  (stage/show-player-msg text))

(defmulti ^:private on-clicked
  (fn [eid]
    (:type (:entity/clickable @eid))))

(defmethod on-clicked :clickable/item [eid]
  (let [item (:entity/item @eid)]
    (cond
     (actor/visible? (stage/get-inventory))
     (do
      (play-sound "bfxr_takeit")
      (swap! eid assoc :entity/destroyed? true)
      (fsm/event player-eid :pickup-item item))

     (inventory/can-pickup-item? @player-eid item)
     (do
      (play-sound "bfxr_pickup")
      (swap! eid assoc :entity/destroyed? true)
      (inventory/pickup-item player-eid item))

     :else
     (do
      (play-sound "bfxr_denied")
      (stage/show-player-msg "Your Inventory is full")))))

(defmethod on-clicked :clickable/player [_]
  (actor/toggle-visible! (stage/get-inventory)))

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
               (actor/user-object (.getParent actor)))))

(defn- mouseover-actor->cursor []
  (let [actor (stage/mouse-on-actor?)]
    (cond
     (inventory-cell-with-item? actor) :cursors/hand-before-grab
     (window-title-bar? actor)      :cursors/move-window
     (button? actor)                :cursors/over-button
     :else                             :cursors/default)))

(defn- player-effect-ctx [eid]
  (let [target-position (or (and mouseover-eid (:position @mouseover-eid))
                            (g/world-mouse-position))]
    {:effect/source eid
     :effect/target mouseover-eid
     :effect/target-position target-position
     :effect/target-direction (v/direction (:position @eid) target-position)}))

(defn- interaction-state [eid]
  (let [entity @eid]
    (cond
     (stage/mouse-on-actor?)
     [(mouseover-actor->cursor) (fn [] nil)] ; handled by actors themself, they check player state

     (and mouseover-eid (:entity/clickable @mouseover-eid))
     (clickable-entity-interaction entity mouseover-eid)

     :else
     (if-let [skill-id (stage/selected-skill)]
       (let [skill (skill-id (:entity/skills entity))
             effect-ctx (player-effect-ctx eid)
             state (skill/usable-state entity skill effect-ctx)]
         (if (= state :usable)
           (do
            ; TODO cursor AS OF SKILL effect (SWORD !) / show already what the effect would do ? e.g. if it would kill highlight
            ; different color ?
            ; => e.g. meditation no TARGET .. etc.
            [:cursors/use-skill
             (fn [] (fsm/event eid :start-action [skill effect-ctx]))])
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

(def ^:private shout-radius 4)

(defn- friendlies-in-radius [position faction]
  (->> {:position position
        :radius shout-radius}
       grid/circle->entities
       (filter #(= (:entity/faction @%) faction))))

(defn- move-position [position {:keys [direction speed delta-time]}]
  (mapv #(+ %1 (* %2 speed delta-time)) position direction))

(defn- move-body [body movement]
  (-> body
      (update :position    move-position movement)
      (update :left-bottom move-position movement)))

(defn- valid-position? [{:keys [entity/id z-order] :as body}]
  {:pre [(:collides? body)]}
  (let [cells* (into [] (map deref) (grid/rectangle->cells body))]
    (and (not-any? #(grid/cell-blocked? % z-order) cells*)
         (->> cells*
              grid/cells->entities
              (not-any? (fn [other-entity]
                          (let [other-entity @other-entity]
                            (and (not= (:entity/id other-entity) id)
                                 (:collides? other-entity)
                                 (body/collides? other-entity body)))))))))

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
(def ^:private max-speed (/ entity/minimum-body-size
                            time/max-delta)) ; need to make var because m/schema would fail later if divide / is inside the schema-form

(def speed-schema (m/schema [:and number? [:>= 0] [:<= max-speed]]))

; FIXME config/changeable inside the app (dev-menu ?)
(def ^:private ^:dbg-flag pausing? true)

(defsystem tick)
(defmethod tick :default [_ eid])

(defmethod tick :entity/animation [[k animation] eid]
  (swap! eid #(-> %
                  (assoc :entity/image (animation/current-frame animation))
                  (assoc k (animation/tick animation time/delta)))))

(defmethod tick :entity/delete-after-animation-stopped? [_ eid]
  (when (animation/stopped? (:entity/animation @eid))
    (swap! eid assoc :entity/destroyed? true)))

(defmethod tick :entity/movement
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
    (let [movement (assoc movement :delta-time time/delta)
          body @eid]
      (when-let [body (if (:collides? body) ; < == means this is a movement-type ... which could be a multimethod ....
                        (try-move-solid-body body movement)
                        (move-body body movement))]
        (entity/position-changed eid)
        (swap! eid assoc
               :position (:position body)
               :left-bottom (:left-bottom body))
        (when rotate-in-movement-direction?
          (swap! eid assoc :rotation-angle (v/angle-from-vector direction)))))))

(defmethod tick :entity/alert-friendlies-after-duration [[_ {:keys [counter faction]}] eid]
  (when (stopped? counter)
    (swap! eid assoc :entity/destroyed? true)
    (doseq [friendly-eid (friendlies-in-radius (:position @eid) faction)]
      (fsm/event friendly-eid :alert))))

(defmethod tick :entity/delete-after-duration [[_ counter] eid]
  (when (stopped? counter)
    (swap! eid assoc :entity/destroyed? true)))

(defmethod tick :entity/string-effect [[k {:keys [counter]}] eid]
  (when (stopped? counter)
    (swap! eid dissoc k)))

(defmethod tick :entity/temp-modifier [[k {:keys [modifiers counter]}] eid]
  (when (stopped? counter)
    (swap! eid dissoc k)
    (swap! eid mods/remove modifiers)))

(defmethod tick :entity/projectile-collision
  [[k {:keys [entity-effects already-hit-bodies piercing?]}] eid]
  ; TODO this could be called from body on collision
  ; for non-solid
  ; means non colliding with other entities
  ; but still collding with other stuff here ? o.o
  (let [entity @eid
        cells* (map deref (grid/rectangle->cells entity)) ; just use cached-touched -cells
        hit-entity (find-first #(and (not (contains? already-hit-bodies %)) ; not filtering out own id
                                     (not= (:entity/faction entity) ; this is not clear in the componentname & what if they dont have faction - ??
                                           (:entity/faction @%))
                                     (:collides? @%)
                                     (body/collides? entity @%))
                               (grid/cells->entities cells*))
        destroy? (or (and hit-entity (not piercing?))
                     (some #(grid/cell-blocked? % (:z-order entity)) cells*))]
    (when destroy?
      (swap! eid assoc :entity/destroyed? true))
    (when hit-entity
      (swap! eid assoc-in [k :already-hit-bodies] (conj already-hit-bodies hit-entity))) ; this is only necessary in case of not piercing ...
    (when hit-entity
      (effect/do-all! {:effect/source eid
                       :effect/target hit-entity}
                      entity-effects))))

(defmethod tick :entity/skills [[k skills] eid]
  (doseq [{:keys [skill/cooling-down?] :as skill} (vals skills)
          :when (and cooling-down?
                     (stopped? cooling-down?))]
    (swap! eid assoc-in [k (:property/id skill) :skill/cooling-down?] false)))

(defmethod tick :stunned [[_ {:keys [counter]}] eid]
  (when (stopped? counter)
    (fsm/event eid :effect-wears-off)))

(defmethod tick :player-moving [[_ {:keys [movement-vector]}] eid]
  (if-let [movement-vector (controls/movement-vector)]
    (swap! eid assoc :entity/movement {:direction movement-vector
                                       :speed (stat/->value @eid :entity/movement-speed)})
    (fsm/event eid :no-movement-input)))

(defmethod tick :npc-sleeping [_ eid]
  (let [entity @eid
        cell (grid/get (body/tile entity))] ; pattern!
    (when-let [distance (grid/nearest-entity-distance @cell (faction/enemy entity))]
      (when (<= distance (stat/->value entity :entity/aggro-range))
        (fsm/event eid :alert)))))

(defmethod tick :npc-moving [[_ {:keys [counter]}] eid]
  (when (stopped? counter)
    (fsm/event eid :timer-finished)))

(defmethod tick :npc-idle [_ eid]
  (let [effect-ctx (npc-effect-ctx eid)]
    (if-let [skill (npc-choose-skill @eid effect-ctx)]
      (fsm/event eid :start-action [skill effect-ctx])
      (fsm/event eid :movement-direction (or (potential-field/find-direction eid) [0 0])))))

(defmethod tick :active-skill [[_ {:keys [skill effect-ctx counter]}] eid]
  (cond
   (not (effect/some-applicable? (effect/check-update-ctx effect-ctx)
                                 (:skill/effects skill)))
   (do
    (fsm/event eid :action-done)
    ; TODO some sound ?
    )

   (stopped? counter)
   (do
    (effect/do-all! effect-ctx (:skill/effects skill))
    (fsm/event eid :action-done))))

; precaution in case a component gets removed by another component
; the question is do we still want to update nil components ?
; should be contains? check ?
; but then the 'order' is important? in such case dependent components
; should be moved together?
(defn- tick-entity [eid]
  (try
   (doseq [k (keys @eid)]
     (try (when-let [v (k @eid)]
            (tick [k v] eid))
          (catch Throwable t
            (throw (ex-info "entity-tick" {:k k} t)))))
   (catch Throwable t
     (throw (ex-info "" (select-keys @eid [:entity/id]) t)))))

(defn- tick-entities [entities]
  (run! tick-entity entities))

(defn- time-update []
  (let [delta-ms (min (g/delta-time) time/max-delta)]
    (alter-var-root #'time/elapsed + delta-ms)
    (bind-root time/delta delta-ms)))

(defn- calculate-eid []
  (let [player @player-eid
        hits (remove #(= (:z-order @%) :z-order/effect)
                     (grid/point->entities
                      (g/world-mouse-position)))]
    (->> render-z-order
         (sort-by-order hits #(:z-order @%))
         reverse
         (filter #(line-of-sight? player @%))
         first)))

(defn- update-mouseover-entity []
  (let [new-eid (if (stage/mouse-on-actor?)
                  nil
                  (calculate-eid))]
    (when mouseover-eid
      (swap! mouseover-eid dissoc :entity/mouseover?))
    (when new-eid
      (swap! new-eid assoc :entity/mouseover? true))
    (bind-root mouseover-eid new-eid)))

(defsystem destroy)
(defmethod destroy :default [_ eid])

(defmethod destroy :entity/destroy-audiovisual [[_ audiovisuals-id] eid]
  (entity/audiovisual (:position @eid)
                      (db/build audiovisuals-id)))

(defn- remove-destroyed-entities []
  (doseq [eid (filter (comp :entity/destroyed? deref)
                      (entity/all-entities))]
    (entity/remove-entity eid)
    (doseq [component @eid]
      (destroy component eid))))

(defsystem manual-tick)
(defmethod manual-tick :default [_])

(defmethod manual-tick :player-item-on-cursor [[_ {:keys [eid]}]]
  (when (and (button-just-pressed? :left)
             (world-item?))
    (fsm/event eid :drop-item)))

(defmethod manual-tick :player-idle [[_ {:keys [eid]}]]
  (if-let [movement-vector (controls/movement-vector)]
    (fsm/event eid :movement-input movement-vector)
    (let [[cursor on-click] (interaction-state eid)]
      (g/set-cursor cursor)
      (when (button-just-pressed? :left)
        (on-click)))))

(defsystem pause-game?)
(defmethod pause-game? :default [_])

(defmethod pause-game? :active-skill          [_] false)
(defmethod pause-game? :stunned               [_] false)
(defmethod pause-game? :player-moving         [_] false)
(defmethod pause-game? :player-item-on-cursor [_] true)
(defmethod pause-game? :player-idle           [_] true)
(defmethod pause-game? :player-dead           [_] true)

(defn update-world []
  (manual-tick (fsm/state-obj @player-eid))
  (update-mouseover-entity) ; this do always so can get debug info even when game not running
  (bind-root time/paused? (or error/throwable
                              (and pausing?
                                   (pause-game? (fsm/state-obj @player-eid))
                                   (not (controls/unpaused?)))))
  (when-not time/paused?
    (time-update)
    (let [entities (entity/active-entities)]
      (update-potential-fields! entities)
      (try (tick-entities entities)
           (catch Throwable t
             (stage/error-window! t)
             (bind-root error/throwable t)))))
  (remove-destroyed-entities)) ; do not pause this as for example pickup item, should be destroyed.
