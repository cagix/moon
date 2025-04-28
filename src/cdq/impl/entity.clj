(ns cdq.impl.entity
  (:require [cdq.audio.sound :as sound]
            [cdq.context :as context]
            [cdq.db :as db]
            [cdq.effect :as effect]
            [cdq.entity :as entity :refer [tick!]]
            cdq.fsm
            [cdq.inventory :as inventory]
            [cdq.input :as input]
            [cdq.graphics :as graphics]
            [cdq.graphics.animation :as animation]
            [cdq.grid :as grid]
            [cdq.line-of-sight :as los]
            cdq.time
            [cdq.timer :as timer]
            [cdq.utils :refer [safe-merge find-first]]
            [cdq.schema :as schema]
            [cdq.skill :as skill]
            [cdq.tx :as tx]
            [cdq.math.vector2 :as v]
            [cdq.widgets.inventory :as widgets.inventory
             :refer [remove-item
                     set-item
                     stack-item]]
            [cdq.world :refer [minimum-size
                               nearest-enemy
                               friendlies-in-radius
                               delayed-alert
                               spawn-audiovisual
                               spawn-item
                               item-place-position
                               world-item?]]
            [cdq.world.potential-field :as potential-field]))

(defmethod entity/create :entity/delete-after-duration
  [[_ duration]
   {:keys [cdq.context/elapsed-time] :as c}]
  (timer/create elapsed-time duration))

(defmethod entity/create :entity/hp
  [[_ v] _c]
  [v v])

(defmethod entity/create :entity/mana
  [[_ v] _c]
  [v v])

(defmethod entity/create :entity/projectile-collision
  [[_ v] c]
  (assoc v :already-hit-bodies #{}))

(defn- apply-action-speed-modifier [entity skill action-time]
  (/ action-time
     (or (entity/stat entity (:skill/action-time-modifier-key skill))
         1)))

(defmethod entity/create :active-skill
  [[_ eid [skill effect-ctx]]
   {:keys [cdq.context/elapsed-time]}]
  {:eid eid
   :skill skill
   :effect-ctx effect-ctx
   :counter (->> skill
                 :skill/action-time
                 (apply-action-speed-modifier @eid skill)
                 (timer/create elapsed-time))})

(defmethod entity/create :npc-dead
  [[_ eid] c]
  {:eid eid})

(defmethod entity/create :npc-idle
  [[_ eid] c]
  {:eid eid})

(defmethod entity/create :npc-moving
  [[_ eid movement-vector]
   {:keys [cdq.context/elapsed-time]}]
  {:eid eid
   :movement-vector movement-vector
   :counter (timer/create elapsed-time (* (entity/stat @eid :entity/reaction-time) 0.016))})

(defmethod entity/create :npc-sleeping
  [[_ eid] c]
  {:eid eid})

(defmethod entity/create :player-dead
  [[k] {:keys [cdq/db] :as c}]
  (db/build db :player-dead/component.enter c))

(defmethod entity/create :player-idle
  [[_ eid] {:keys [cdq/db] :as c}]
  (safe-merge (db/build db :player-idle/clicked-inventory-cell c)
              {:eid eid}))

(defmethod entity/create :player-item-on-cursor
  [[_ eid item] {:keys [cdq/db] :as c}]
  (safe-merge (db/build db :player-item-on-cursor/component c)
              {:eid eid
               :item item}))

(defmethod entity/create :player-moving
  [[_ eid movement-vector] c]
  {:eid eid
   :movement-vector movement-vector})

(defmethod entity/create :stunned
  [[_ eid duration]
   {:keys [cdq.context/elapsed-time]}]
  {:eid eid
   :counter (timer/create elapsed-time duration)})

(defmethod entity/create! :entity/inventory
  [[k items] eid c]
  (swap! eid assoc k inventory/empty-inventory)
  (doseq [item items]
    (widgets.inventory/pickup-item c eid item)))

(defmethod entity/create! :entity/skills
  [[k skills] eid c]
  (swap! eid assoc k nil)
  (doseq [skill skills]
    (tx/add-skill c eid skill)))

(defmethod entity/create! :entity/animation
  [[_ animation] eid c]
  (swap! eid assoc :entity/image (animation/current-frame animation)))

(defmethod entity/create! :entity/delete-after-animation-stopped?
  [_ eid c]
  (-> @eid :entity/animation :looping? not assert))

(defmethod entity/create! :entity/fsm
  [[k {:keys [fsm initial-state]}] eid c]
  (swap! eid assoc
         k (cdq.fsm/create fsm initial-state)
         initial-state (entity/create [initial-state eid] c)))

(defmethod entity/draw-gui-view :player-item-on-cursor
  [[_ {:keys [eid]}] {:keys [cdq.graphics/ui-viewport] :as c}]
  (when (not (world-item? c))
    (graphics/draw-centered c
                            (:entity/image (:entity/item-on-cursor @eid))
                            (graphics/mouse-position ui-viewport))))

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
   (not (effect/some-applicable? (update-effect-ctx c effect-ctx)
                                 (:skill/effects skill)))
   (do
    (tx/event c eid :action-done)
    ; TODO some sound ?
    )

   (timer/stopped? counter elapsed-time)
   (do
    (tx/effect c effect-ctx (:skill/effects skill))
    (tx/event c eid :action-done))))

(defn- npc-choose-skill [c entity ctx]
  (->> entity
       :entity/skills
       vals
       (sort-by #(or (:skill/cost %) 0))
       reverse
       (filter #(and (= :usable (skill/usable-state entity % ctx))
                     (effect/applicable-and-useful? c ctx (:skill/effects %))))
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
      (tx/event c eid :start-action [skill effect-ctx])
      (tx/event c eid :movement-direction (or (potential-field/find-direction c eid) [0 0])))))

(defmethod tick! :npc-moving [[_ {:keys [counter]}]
                              eid
                              {:keys [cdq.context/elapsed-time] :as c}]
  (when (timer/stopped? counter elapsed-time)
    (tx/event c eid :timer-finished)))

(defmethod tick! :npc-sleeping [_ eid {:keys [cdq.context/grid] :as c}]
  (let [entity @eid
        cell (grid (entity/tile entity))]
    (when-let [distance (grid/nearest-entity-distance @cell (entity/enemy entity))]
      (when (<= distance (entity/stat entity :entity/aggro-range))
        (tx/event c eid :alert)))))

(defmethod tick! :player-moving [[_ {:keys [movement-vector]}] eid c]
  (if-let [movement-vector (input/player-movement-vector)]
    (tx/set-movement eid movement-vector)
    (tx/event c eid :no-movement-input)))

(defmethod tick! :stunned [[_ {:keys [counter]}]
                           eid
                           {:keys [cdq.context/elapsed-time] :as c}]
  (when (timer/stopped? counter elapsed-time)
    (tx/event c eid :effect-wears-off)))

(defmethod tick! :entity/alert-friendlies-after-duration
  [[_ {:keys [counter faction]}]
   eid
   {:keys [cdq.context/grid
           cdq.context/elapsed-time]
    :as c}]
  (when (timer/stopped? counter elapsed-time)
    (tx/mark-destroyed eid)
    (doseq [friendly-eid (friendlies-in-radius grid (:position @eid) faction)]
      (tx/event c friendly-eid :alert))))

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
    (tx/mark-destroyed eid)))

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

(def ^:private speed-schema (schema/m-schema [:and number? [:>= 0] [:<= max-speed]]))

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
      (tx/mark-destroyed eid))
    (when hit-entity
      (swap! eid assoc-in [k :already-hit-bodies] (conj already-hit-bodies hit-entity))) ; this is only necessary in case of not piercing ...
    (when hit-entity
      (tx/effect c
                 {:effect/source eid
                  :effect/target hit-entity}
                 entity-effects))))

(defmethod tick! :entity/delete-after-animation-stopped?
  [_ eid c]
  (when (animation/stopped? (:entity/animation @eid))
    (tx/mark-destroyed eid)))

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

(def ^:private components
  {:entity/destroy-audiovisual {:destroy! (fn [audiovisuals-id eid {:keys [cdq/db] :as c}]
                                            (spawn-audiovisual c
                                                               (:position @eid)
                                                               (db/build db audiovisuals-id c)))}
   :player-idle           {:pause-game? true}
   :active-skill          {:pause-game? false
                           :cursor :cursors/sandclock
                           :enter (fn [[_ {:keys [eid skill]}]
                                       {:keys [cdq.context/elapsed-time] :as c}]
                                    (sound/play (:skill/start-action-sound skill))
                                    (when (:skill/cooldown skill)
                                      (swap! eid assoc-in
                                             [:entity/skills (:property/id skill) :skill/cooling-down?]
                                             (timer/create elapsed-time (:skill/cooldown skill))))
                                    (when (and (:skill/cost skill)
                                               (not (zero? (:skill/cost skill))))
                                      (swap! eid entity/pay-mana-cost (:skill/cost skill))))}
   :player-dead           {:pause-game? true
                           :cursor :cursors/black-x
                           :enter (fn [[_ {:keys [tx/sound
                                                  modal/title
                                                  modal/text
                                                  modal/button-text]}]
                                       c]
                                    (sound/play sound)
                                    (tx/show-modal c {:title title
                                                      :text text
                                                      :button-text button-text
                                                      :on-click (fn [])}))}
   :player-item-on-cursor {:pause-game? true
                           :cursor :cursors/hand-grab
                           :enter (fn [[_ {:keys [eid item]}] c]
                                    (swap! eid assoc :entity/item-on-cursor item))
                           :exit (fn [[_ {:keys [eid player-item-on-cursor/place-world-item-sound]}] c]
                                   ; at clicked-cell when we put it into a inventory-cell
                                   ; we do not want to drop it on the ground too additonally,
                                   ; so we dissoc it there manually. Otherwise it creates another item
                                   ; on the ground
                                   (let [entity @eid]
                                     (when (:entity/item-on-cursor entity)
                                       (sound/play place-world-item-sound)
                                       (swap! eid dissoc :entity/item-on-cursor)
                                       (spawn-item c
                                                   (item-place-position c entity)
                                                   (:entity/item-on-cursor entity)))))}
   :player-moving         {:pause-game? false
                           :cursor :cursors/walking
                           :enter (fn [[_ {:keys [eid movement-vector]}] c]
                                    (tx/set-movement eid movement-vector))
                           :exit (fn [[_ {:keys [eid]}] c]
                                   (swap! eid dissoc :entity/movement))}
   :stunned               {:pause-game? false
                           :cursor :cursors/denied}
   :npc-dead              {:enter (fn [[_ {:keys [eid]}] c]
                                    (tx/mark-destroyed eid))}
   :npc-moving            {:enter (fn [[_ {:keys [eid movement-vector]}] c]
                                    (tx/set-movement eid movement-vector))
                           :exit (fn [[_ {:keys [eid]}] c]
                                   (swap! eid dissoc :entity/movement))}
   :npc-sleeping          {:exit (fn [[_ {:keys [eid]}] c]
                                   (delayed-alert c
                                                  (:position       @eid)
                                                  (:entity/faction @eid)
                                                  0.2)
                                   (tx/text-effect c eid "[WHITE]!"))}})

(defn add-components [context _config]
  (assoc context :context/entity-components components))

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
      (sound/play item-put-sound)
      (swap! eid dissoc :entity/item-on-cursor)
      (set-item c eid cell item-on-cursor)
      (tx/event c eid :dropped-item))

     ; STACK ITEMS
     (and item-in-cell
          (inventory/stackable? item-in-cell item-on-cursor))
     (do
      (sound/play item-put-sound)
      (swap! eid dissoc :entity/item-on-cursor)
      (stack-item c eid cell item-on-cursor)
      (tx/event c eid :dropped-item))

     ; SWAP ITEMS
     (and item-in-cell
          (inventory/valid-slot? cell item-on-cursor))
     (do
      (sound/play item-put-sound)
      ; need to dissoc and drop otherwise state enter does not trigger picking it up again
      ; TODO? coud handle pickup-item from item-on-cursor state also
      (swap! eid dissoc :entity/item-on-cursor)
      (remove-item c eid cell)
      (set-item c eid cell item-on-cursor)
      (tx/event c eid :dropped-item)
      (tx/event c eid :pickup-item item-in-cell)))))

(defmethod entity/clicked-inventory-cell :player-item-on-cursor
  [[_ {:keys [eid] :as data}] cell c]
  (clicked-cell data eid cell c))

(defmethod entity/clicked-inventory-cell :player-idle
  [[_ {:keys [eid player-idle/pickup-item-sound]}] cell c]
  ; TODO no else case
  (when-let [item (get-in (:entity/inventory @eid) cell)]
    (sound/play pickup-item-sound)
    (tx/event c eid :pickup-item item)
    (remove-item c eid cell)))
