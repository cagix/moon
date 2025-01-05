(ns cdq.context
  (:require [anvil.world.potential-field :as potential-field]
            [cdq.effect :as effect]
            [cdq.inventory :as inventory]
            [gdl.context.timer :as timer]
            [gdl.graphics.animation :as animation]
            [gdl.malli :as m]
            [cdq.widgets.inventory :as widgets.inventory]
            [cdq.fsm :as fsm]
            [cdq.entity :as entity]
            [gdl.error :refer [pretty-pst]]
            [cdq.entity.state :as state]
            [cdq.content-grid :as content-grid]
            [cdq.grid :as grid]
            [clojure.gdx :as gdx :refer [play key-pressed? key-just-pressed? clear-screen black button-just-pressed?]]
            [clojure.gdx.scene2d.actor :as actor]
            [clojure.gdx.scene2d.ui.button-group :as button-group]
            [gdl.utils :refer [defsystem defcomponent readable-number dev-mode? define-order sort-by-order safe-merge find-first rand-int-between]]
            [cdq.effect-context :as effect-ctx]
            [anvil.skill :as skill]
            [gdl.context :as c :refer [play-sound]]
            [cdq.context.info :as info]
            [gdl.graphics.camera :as cam]
            [gdl.math.raycaster :as raycaster]
            [cdq.potential-fields :as potential-fields]
            [clojure.gdx.math.vector2 :as v]
            [gdl.ui :as ui :refer [ui-actor]]
            [clojure.gdx.scene2d.actor :as actor]
            [clojure.gdx.scene2d.group :as group]))

; so that at low fps the game doesn't jump faster between frames used @ movement to set a max speed so entities don't jump over other entities when checking collisions
(def max-delta-time 0.04)

(defn grid-cell [{::keys [grid]} position]
  (grid position))

(defn rectangle->cells [{::keys [grid]} rectangle]
  (grid/rectangle->cells grid rectangle))

(defn circle->cells [{::keys [grid]} circle]
  (grid/circle->cells grid circle))

(defn circle->entities [{::keys [grid]} circle]
  (grid/circle->entities grid circle))

(defn cached-adjacent-cells [{::keys [grid]} cell]
  (grid/cached-adjacent-cells grid cell))

(defn point->entities [{::keys [grid]} position]
  (grid/point->entities grid position))

(defn ray-blocked? [{::keys [raycaster]} start target]
  (raycaster/blocked? raycaster start target))

(defn path-blocked?
  "path-w in tiles. casts two rays."
  [{::keys [raycaster]} start target path-w]
  (raycaster/path-blocked? raycaster start target path-w))

(defn mouseover-entity [{::keys [mouseover-eid]}]
  (and mouseover-eid
       @mouseover-eid))

(defn all-entities [{::keys [entity-ids]}]
  (vals @entity-ids))

; does not take into account zoom - but zoom is only for debug ???
; vision range?
(defn- on-screen? [viewport entity]
  (let [[x y] (:position entity)
        x (float x)
        y (float y)
        [cx cy] (cam/position (:camera viewport))
        px (float cx)
        py (float cy)
        xdist (Math/abs (- x px))
        ydist (Math/abs (- y py))]
    (and
     (<= xdist (inc (/ (float (:width viewport))  2)))
     (<= ydist (inc (/ (float (:height viewport)) 2))))))

; TODO at wrong point , this affects targeting logic of npcs
; move the debug flag to either render or mouseover or lets see
(def ^:private ^:dbg-flag los-checks? true)

; does not take into account size of entity ...
; => assert bodies <1 width then
(defn line-of-sight? [{:keys [gdl.context/world-viewport] :as c} source target]
  (and (or (not (:entity/player? source))
           (on-screen? world-viewport target))
       (not (and los-checks?
                 (ray-blocked? c (:position source) (:position target))))))

; this as protocols & impl implements it? same with send-event ?
; so we could add those protocols to 'entity'?
; => also add render stuff
; so each component is together all stuff (but question is if we have open data)
(defn add-text-effect [entity c text]
  (assoc entity
         :entity/string-effect
         (if-let [string-effect (:entity/string-effect entity)]
           (-> string-effect
               (update :text str "\n" text)
               (update :counter #(timer/reset c %)))
           {:text text
            :counter (timer/create c 0.4)})))

(defn active-entities [{::keys [content-grid player-eid]}]
  (content-grid/active-entities content-grid @player-eid))

(defn- add-entity [{::keys [content-grid grid entity-ids]} eid]
  ; https://github.com/damn/core/issues/58
  ;(assert (valid-position? grid @eid)) ; TODO deactivate because projectile no left-bottom remove that field or update properly for all
  (content-grid/add-entity content-grid eid)
  (let [id (:entity/id @eid)]
    (assert (number? id))
    (swap! entity-ids assoc id eid))
  (grid/add-entity grid eid))

(defn remove-entity [{::keys [entity-ids]} eid]
  (content-grid/remove-entity eid)
  (let [id (:entity/id @eid)]
    (assert (contains? @entity-ids id))
    (swap! entity-ids dissoc id))
  (grid/remove-entity eid))

(defn position-changed [{::keys [content-grid grid]} eid]
  (content-grid/entity-position-changed content-grid eid)
  (grid/entity-position-changed grid eid))

; setting a min-size for colliding bodies so movement can set a max-speed for not
; skipping bodies at too fast movement
; TODO assert at properties load
(def minimum-size 0.39) ; == spider smallest creature size.

(def ^:private z-orders [:z-order/on-ground
                         :z-order/ground
                         :z-order/flying
                         :z-order/effect])

(def render-z-order (define-order z-orders))

(defrecord Body [position
                 left-bottom
                 width
                 height
                 half-width
                 half-height
                 radius
                 collides?
                 z-order
                 rotation-angle])

(defn- create-body [{[x y] :position
                     :keys [position
                            width
                            height
                            collides?
                            z-order
                            rotation-angle]}]
  (assert position)
  (assert width)
  (assert height)
  (assert (>= width  (if collides? minimum-size 0)))
  (assert (>= height (if collides? minimum-size 0)))
  (assert (or (boolean? collides?) (nil? collides?)))
  (assert ((set z-orders) z-order))
  (assert (or (nil? rotation-angle)
              (<= 0 rotation-angle 360)))
  (map->Body
   {:position (mapv float position)
    :left-bottom [(float (- x (/ width  2)))
                  (float (- y (/ height 2)))]
    :width  (float width)
    :height (float height)
    :half-width  (float (/ width  2))
    :half-height (float (/ height 2))
    :radius (float (max (/ width  2)
                        (/ height 2)))
    :collides? collides?
    :z-order z-order
    :rotation-angle (or rotation-angle 0)}))

(defn- create-vs [components context]
  (reduce (fn [m [k v]]
            (assoc m k (entity/create [k v] context)))
          {}
          components))

(defsystem create!)
(defmethod create! :default [_ eid c])

(let [cnt (atom 0)]
  (defn spawn-entity [c position body components]
    (assert (and (not (contains? components :position))
                 (not (contains? components :entity/id))))
    (let [eid (atom (-> body
                        (assoc :position position)
                        create-body
                        (safe-merge (-> components
                                        (assoc :entity/id (swap! cnt inc))
                                        (create-vs c)))))]
      (add-entity c eid)
      (doseq [component @eid]
        (create! component eid c))
      eid)))

(def ^{:doc "For effects just to have a mouseover body size for debugging purposes."
       :private true}
  effect-body-props
  {:width 0.5
   :height 0.5
   :z-order :z-order/effect})

(defn spawn-audiovisual [c position {:keys [tx/sound entity/animation]}]
  (play sound)
  (spawn-entity c
                position
                effect-body-props
                {:entity/animation animation
                 :entity/delete-after-animation-stopped? true}))

; # :z-order/flying has no effect for now
; * entities with :z-order/flying are not flying over water,etc. (movement/air)
; because using potential-field for z-order/ground
; -> would have to add one more potential-field for each faction for z-order/flying
; * they would also (maybe) need a separate occupied-cells if they don't collide with other
; * they could also go over ground units and not collide with them
; ( a test showed then flying OVER player entity )
; -> so no flying units for now
(defn- ->body [{:keys [body/width body/height #_body/flying?]}]
  {:width  width
   :height height
   :collides? true
   :z-order :z-order/ground #_(if flying? :z-order/flying :z-order/ground)})

(defn spawn-creature [c {:keys [position creature-id components]}]
  (let [props (c/build c creature-id)]
    (spawn-entity c
                  position
                  (->body (:entity/body props))
                  (-> props
                      (dissoc :entity/body)
                      (assoc :entity/destroy-audiovisual :audiovisuals/creature-die)
                      (safe-merge components)))))

(defn spawn-item [c position item]
  (spawn-entity c
                position
                {:width 0.75
                 :height 0.75
                 :z-order :z-order/on-ground}
                {:entity/image (:entity/image item)
                 :entity/item item
                 :entity/clickable {:type :clickable/item
                                    :text (:property/pretty-name item)}}))

(defn delayed-alert [c position faction duration]
  (spawn-entity c
                position
                effect-body-props
                {:entity/alert-friendlies-after-duration
                 {:counter (timer/create c duration)
                  :faction faction}}))

(defn line-render [c {:keys [start end duration color thick?]}]
  (spawn-entity c
                start
                effect-body-props
                #:entity {:line-render {:thick? thick? :end end :color color}
                          :delete-after-duration duration}))

(defn projectile-size [projectile]
  {:pre [(:entity/image projectile)]}
  (first (:world-unit-dimensions (:entity/image projectile))))

(defn spawn-projectile [c
                        {:keys [position direction faction]}
                        {:keys [entity/image
                                projectile/max-range
                                projectile/speed
                                entity-effects
                                projectile/piercing?] :as projectile}]
  (let [size (projectile-size projectile)]
    (spawn-entity c
                  position
                  {:width size
                   :height size
                   :z-order :z-order/flying
                   :rotation-angle (v/angle-from-vector direction)}
                  {:entity/movement {:direction direction
                                     :speed speed}
                   :entity/image image
                   :entity/faction faction
                   :entity/delete-after-duration (/ max-range speed)
                   :entity/destroy-audiovisual :audiovisuals/hit-wall
                   :entity/projectile-collision {:entity-effects entity-effects
                                                 :piercing? piercing?}})))

(defn creatures-in-los-of-player [{::keys [player-eid] :as c}]
  (->> (active-entities c)
       (filter #(:entity/species @%))
       (filter #(line-of-sight? c @player-eid @%))
       (remove #(:entity/player? @%))))

(def ^:private shout-radius 4)

(defn friendlies-in-radius [c position faction]
  (->> {:position position
        :radius shout-radius}
       (circle->entities c)
       (filter #(= (:entity/faction @%) faction))))

(defn nearest-enemy [{::keys [grid]} entity]
  (grid/nearest-entity @(grid (entity/tile entity))
                       (entity/enemy entity)))

(defn get-inventory [c]
  (get (:windows (c/stage c)) :inventory-window))

(defn get-action-bar [c]
  (let [group (:ui/action-bar (:action-bar-table (c/stage c)))]
    {:horizontal-group group
     :button-group (actor/user-object (group/find-actor group "action-bar/button-group"))}))

(defn- action-bar-add-skill [c {:keys [property/id entity/image] :as skill}]
  (let [{:keys [horizontal-group button-group]} (get-action-bar c)
        button (ui/image-button image (fn []) {:scale 2})]
    (actor/set-id button id)
    (ui/add-tooltip! button #(info/text % skill)) ; (assoc ctx :effect/source (world/player)) FIXME
    (group/add-actor! horizontal-group button)
    (button-group/add button-group button)
    nil))

(defn- action-bar-remove-skill [c {:keys [property/id]}]
  (let [{:keys [horizontal-group button-group]} (get-action-bar c)
        button (get horizontal-group id)]
    (actor/remove button)
    (button-group/remove button-group button)
    nil))

(defn selected-skill [c]
  (when-let [skill-button (button-group/checked (:button-group (get-action-bar c)))]
    (actor/user-object skill-button)))

(defn show-player-msg [{::keys [player-message]} text]
  (swap! player-message assoc :text text :counter 0))

; no window movable type cursor appears here like in player idle
; inventory still working, other stuff not, because custom listener to keypresses ? use actor listeners?
; => input events handling
; hmmm interesting ... can disable @ item in cursor  / moving / etc.
(defn show-modal [{:keys [gdl.context/viewport] :as c}
                  {:keys [title text button-text on-click]}]
  (assert (not (::modal (c/stage c))))
  (c/add-actor c
               (ui/window {:title title
                           :rows [[(ui/label text)]
                                  [(ui/text-button button-text
                                                   (fn []
                                                     (actor/remove (::modal (c/stage c)))
                                                     (on-click)))]]
                           :id ::modal
                           :modal? true
                           :center-position [(/ (:width viewport) 2)
                                             (* (:height viewport) (/ 3 4))]
                           :pack? true})))

(defn world-item? [c]
  (not (c/mouse-on-actor? c)))

; It is possible to put items out of sight, losing them.
; Because line of sight checks center of entity only, not corners
; this is okay, you have thrown the item over a hill, thats possible.
(defn- placement-point [player target maxrange]
  (v/add player
         (v/scale (v/direction player target)
                  (min maxrange
                       (v/distance player target)))))

(defn item-place-position [c entity]
  (placement-point (:position entity)
                   (c/world-mouse-position c)
                   ; so you cannot put it out of your own reach
                   (- (:entity/click-distance-tiles entity) 0.1)))

(defn send-event!
  ([c eid event]
   (send-event! c eid event nil))
  ([c eid event params]
   (when-let [fsm (:entity/fsm @eid)]
     (let [old-state-k (:state fsm)
           new-fsm (fsm/event fsm event)
           new-state-k (:state new-fsm)]
       (when-not (= old-state-k new-state-k)
         (let [old-state-obj (entity/state-obj @eid)
               new-state-obj [new-state-k (entity/create (if params
                                                           [new-state-k eid params]
                                                           [new-state-k eid])
                                                         c)]]
           (when (:entity/player? @eid)
             (when-let [cursor (state/cursor new-state-obj)]
               (c/set-cursor c cursor)))
           (swap! eid #(-> %
                           (assoc :entity/fsm new-fsm
                                  new-state-k (new-state-obj 1))
                           (dissoc old-state-k)))
           (state/exit  old-state-obj c)
           (state/enter new-state-obj c)))))))

(defn set-item [c eid cell item]
  (let [entity @eid
        inventory (:entity/inventory entity)]
    (assert (and (nil? (get-in inventory cell))
                 (inventory/valid-slot? cell item)))
    (when (:entity/player? entity)
      (widgets.inventory/set-item-image-in-widget c cell item))
    (swap! eid assoc-in (cons :entity/inventory cell) item)
    (when (inventory/applies-modifiers? cell)
      (swap! eid entity/mod-add (:entity/modifiers item)))))

(defn remove-item [c eid cell]
  (let [entity @eid
        item (get-in (:entity/inventory entity) cell)]
    (assert item)
    (when (:entity/player? entity)
      (widgets.inventory/remove-item-from-widget c cell))
    (swap! eid assoc-in (cons :entity/inventory cell) nil)
    (when (inventory/applies-modifiers? cell)
      (swap! eid entity/mod-remove (:entity/modifiers item)))))

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
(defn stack-item [c eid cell item]
  (let [cell-item (get-in (:entity/inventory @eid) cell)]
    (assert (inventory/stackable? item cell-item))
    ; TODO this doesnt make sense with modifiers ! (triggered 2 times if available)
    ; first remove and then place, just update directly  item ...
    (concat (remove-item c eid cell)
            (set-item c eid cell (update cell-item :count + (:count item))))))

(defn pickup-item [c eid item]
  (let [[cell cell-item] (entity/can-pickup-item? @eid item)]
    (assert cell)
    (assert (or (inventory/stackable? item cell-item)
                (nil? cell-item)))
    (if (inventory/stackable? item cell-item)
      (stack-item c eid cell item)
      (set-item c eid cell item))))

(defn draw-body-rect [c entity color]
  (let [[x y] (:left-bottom entity)]
    (c/rectangle c x y (:width entity) (:height entity) color)))

(defn handle-player-input [{:keys [cdq.context/player-eid] :as c}]
  (state/manual-tick (entity/state-obj @player-eid) c)
  c)

(defn- calculate-mouseover-eid [{:keys [cdq.context/player-eid] :as c}]
  (let [player @player-eid
        hits (remove #(= (:z-order @%) :z-order/effect)
                     (point->entities c (c/world-mouse-position c)))]
    (->> render-z-order
         (sort-by-order hits #(:z-order @%))
         reverse
         (filter #(line-of-sight? c player @%))
         first)))

(defn update-mouseover-entity [{:keys [cdq.context/mouseover-eid] :as c}]
  (let [new-eid (if (c/mouse-on-actor? c)
                  nil
                  (calculate-mouseover-eid c))]
    (when mouseover-eid
      (swap! mouseover-eid dissoc :entity/mouseover?))
    (when new-eid
      (swap! new-eid assoc :entity/mouseover? true))
    (assoc c :cdq.context/mouseover-eid new-eid)))

(def ^:private ^:dbg-flag pausing? true)

(defn update-paused-state [{:keys [cdq.context/player-eid error] :as c}]
  (assoc c :cdq.context/paused? (or error
                                    (and pausing?
                                         (state/pause-game? (entity/state-obj @player-eid))
                                         (not (or (key-just-pressed? c :p)
                                                  (key-pressed? c :space)))))))

(defn- update-time [c]
  (let [delta-ms (min (gdx/delta-time c) max-delta-time)]
    (-> c
        (update :gdl.context/elapsed-time + delta-ms)
        (assoc :cdq.context/delta-time delta-ms))))

(def ^:private pf-cache (atom nil))

(defn- tick-potential-fields [{:keys [cdq.context/factions-iterations
                                      cdq.context/grid] :as c}]
  (let [entities (active-entities c)]
    (doseq [[faction max-iterations] factions-iterations]
      (potential-fields/tick pf-cache
                             grid
                             faction
                             entities
                             max-iterations)))
  c)

(defsystem tick!)
(defmethod tick! :default [_ eid c])

; precaution in case a component gets removed by another component
; the question is do we still want to update nil components ?
; should be contains? check ?
; but then the 'order' is important? in such case dependent components
; should be moved together?
(defn- tick-entities [c]
  (try (doseq [eid (active-entities c)]
         (try
          (doseq [k (keys @eid)]
            (try (when-let [v (k @eid)]
                   (tick! [k v] eid c))
                 (catch Throwable t
                   (throw (ex-info "entity-tick" {:k k} t)))))
          (catch Throwable t
            (throw (ex-info "" (select-keys @eid [:entity/id]) t)))))
       (catch Throwable t
         (c/error-window c t)
         #_(bind-root ::error t))) ; FIXME ... either reduce or use an atom ...
  c)

(defsystem destroy!)
(defmethod destroy! :default [_ eid c])

(defn remove-destroyed-entities [c]
  (doseq [eid (filter (comp :entity/destroyed? deref)
                      (all-entities c))]
    (remove-entity c eid)
    (doseq [component @eid]
      (destroy! component eid c)))
  c)

(defmethod destroy! :entity/destroy-audiovisual
  [[_ audiovisuals-id] eid c]
  (spawn-audiovisual c
                     (:position @eid)
                     (c/build c audiovisuals-id)))

(def window-hotkeys
  {:inventory-window   :i
   :entity-info-window :e})

(defn- check-window-hotkeys [c]
  (doseq [window-id [:inventory-window
                     :entity-info-window]
          :when (key-just-pressed? c (get window-hotkeys window-id))]
    (actor/toggle-visible! (get (:windows (c/stage c)) window-id))))

(defn- close-all-windows [stage]
  (let [windows (group/children (:windows stage))]
    (when (some actor/visible? windows)
      (run! #(actor/set-visible % false) windows))))

(def close-windows-key :escape)

(defn check-ui-key-listeners [c]
  (check-window-hotkeys c)
  (when (key-just-pressed? c close-windows-key)
    (close-all-windows (c/stage c)))
  c)

(defn player-movement-vector [c]
  (c/WASD-movement-vector c))

(defn add-skill [c eid {:keys [property/id] :as skill}]
  {:pre [(not (entity/has-skill? @eid skill))]}
  (when (:entity/player? @eid)
    (action-bar-add-skill c skill))
  (swap! eid assoc-in [:entity/skills id] skill))

(defn remove-skill [c eid {:keys [property/id] :as skill}]
  {:pre [(entity/has-skill? @eid skill)]}
  (when (:entity/player? @eid)
    (action-bar-remove-skill c skill))
  (swap! eid update :entity/skills dissoc id))

(defmethod create! :entity/inventory
  [[k items] eid c]
  (swap! eid assoc k inventory/empty-inventory)
  (doseq [item items]
    (pickup-item c eid item)))

(defmethod create! :entity/skills
  [[k skills] eid c]
  (swap! eid assoc k nil)
  (doseq [skill skills]
    (add-skill c eid skill)))

(defmethod create! :entity/animation
  [[_ animation] eid c]
  (swap! eid assoc :entity/image (animation/current-frame animation)))

(defmethod create! :entity/delete-after-animation-stopped?
  [_ eid c]
  (-> @eid :entity/animation :looping? not assert))

(defmethod create! :entity/fsm
  [[k {:keys [fsm initial-state]}] eid c]
  (swap! eid assoc
         k (fsm/create fsm initial-state)
         initial-state (entity/create [initial-state eid] c)))

(defmethod tick! :entity/alert-friendlies-after-duration
  [[_ {:keys [counter faction]}] eid c]
  (when (timer/stopped? c counter)
    (swap! eid assoc :entity/destroyed? true)
    (doseq [friendly-eid (friendlies-in-radius c (:position @eid) faction)]
      (send-event! c friendly-eid :alert))))

(defmethod tick! :entity/animation
  [[k animation] eid {:keys [cdq.context/delta-time]}]
  (swap! eid #(-> %
                  (assoc :entity/image (animation/current-frame animation))
                  (assoc k (animation/tick animation delta-time)))))

(defmethod tick! :entity/delete-after-duration
  [[_ counter] eid c]
  (when (timer/stopped? c counter)
    (swap! eid assoc :entity/destroyed? true)))

(defn- move-position [position {:keys [direction speed delta-time]}]
  (mapv #(+ %1 (* %2 speed delta-time)) position direction))

(defn- move-body [body movement]
  (-> body
      (update :position    move-position movement)
      (update :left-bottom move-position movement)))

(defn- valid-position? [c {:keys [entity/id z-order] :as body}]
  {:pre [(:collides? body)]}
  (let [cells* (into [] (map deref) (rectangle->cells c body))]
    (and (not-any? #(grid/blocked? % z-order) cells*)
         (->> cells*
              grid/cells->entities
              (not-any? (fn [other-entity]
                          (let [other-entity @other-entity]
                            (and (not= (:entity/id other-entity) id)
                                 (:collides? other-entity)
                                 (entity/collides? other-entity body)))))))))

(defn- try-move [c body movement]
  (let [new-body (move-body body movement)]
    (when (valid-position? c new-body)
      new-body)))

; TODO sliding threshold
; TODO name - with-sliding? 'on'
; TODO if direction was [-1 0] and invalid-position then this algorithm tried to move with
; direection [0 0] which is a waste of processor power...
(defn- try-move-solid-body [c body {[vx vy] :direction :as movement}]
  (let [xdir (Math/signum (float vx))
        ydir (Math/signum (float vy))]
    (or (try-move c body movement)
        (try-move c body (assoc movement :direction [xdir 0]))
        (try-move c body (assoc movement :direction [0 ydir])))))

; set max speed so small entities are not skipped by projectiles
; could set faster than max-speed if I just do multiple smaller movement steps in one frame
(def ^:private max-speed (/ minimum-size
                            max-delta-time)) ; need to make var because m/schema would fail later if divide / is inside the schema-form

(def speed-schema (m/schema [:and number? [:>= 0] [:<= max-speed]]))

(defmethod tick! :entity/movement
  [[_ {:keys [direction speed rotate-in-movement-direction?] :as movement}]
            eid
            {:keys [cdq.context/delta-time] :as c}]
  (assert (m/validate speed-schema speed)
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
                        (try-move-solid-body c body movement)
                        (move-body body movement))]
        (position-changed c eid)
        (swap! eid assoc
               :position (:position body)
               :left-bottom (:left-bottom body))
        (when rotate-in-movement-direction?
          (swap! eid assoc :rotation-angle (v/angle-from-vector direction)))))))

(defmethod tick! :entity/projectile-collision
  [[k {:keys [entity-effects already-hit-bodies piercing?]}] eid c]
  ; TODO this could be called from body on collision
  ; for non-solid
  ; means non colliding with other entities
  ; but still collding with other stuff here ? o.o
  (let [entity @eid
        cells* (map deref (rectangle->cells c entity)) ; just use cached-touched -cells
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
  [[k skills] eid c]
  (doseq [{:keys [skill/cooling-down?] :as skill} (vals skills)
          :when (and cooling-down?
                     (timer/stopped? c cooling-down?))]
    (swap! eid assoc-in [k (:property/id skill) :skill/cooling-down?] false)))

(defmethod tick! :entity/string-effect
  [[k {:keys [counter]}] eid c]
  (when (timer/stopped? c counter)
    (swap! eid dissoc k)))

(defmethod tick! :entity/temp-modifier
  [[k {:keys [modifiers counter]}] eid c]
  (when (timer/stopped? c counter)
    (swap! eid dissoc k)
    (swap! eid entity/mod-remove modifiers)))

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

  (state/enter [[_ {:keys [eid skill]}] c]
    (play (:skill/start-action-sound skill))
    (when (:skill/cooldown skill)
      (swap! eid assoc-in
             [:entity/skills (:property/id skill) :skill/cooling-down?]
             (timer/create c (:skill/cooldown skill))))
    (when (and (:skill/cost skill)
               (not (zero? (:skill/cost skill))))
      (swap! eid entity/pay-mana-cost (:skill/cost skill))))

  (tick! [[_ {:keys [skill effect-ctx counter]}] eid c]
    (cond
     (not (effect-ctx/some-applicable? (update-effect-ctx c effect-ctx)
                                       (:skill/effects skill)))
     (do
      (send-event! c eid :action-done)
      ; TODO some sound ?
      )

     (timer/stopped? c counter)
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

  (tick! [[_ {:keys [counter]}] eid c]
    (when (timer/stopped? c counter)
      (send-event! c eid :timer-finished))))

(defcomponent :npc-sleeping
  (state/exit [[_ {:keys [eid]}] c]
    (delayed-alert c
                   (:position       @eid)
                   (:entity/faction @eid)
                   0.2)
    (swap! eid add-text-effect c "[WHITE]!"))

  (tick! [_ eid c]
    (let [entity @eid
          cell (grid-cell c (entity/tile entity))] ; pattern!
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
    (play sound)
    (show-modal c {:title title
                   :text text
                   :button-text button-text
                   :on-click (fn [])})))

(defmulti ^:private on-clicked
  (fn [eid c]
    (:type (:entity/clickable @eid))))

(defmethod on-clicked :clickable/item [eid {:keys [cdq.context/player-eid] :as c}]
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

(defn- inventory-cell-with-item? [{:keys [cdq.context/player-eid] :as c} actor]
  (and (actor/parent actor)
       (= "inventory-cell" (.getName (actor/parent actor)))
       (get-in (:entity/inventory @player-eid)
               (actor/user-object (actor/parent actor)))))

(defn- mouseover-actor->cursor [c]
  (let [actor (c/mouse-on-actor? c)]
    (cond
     (inventory-cell-with-item? c actor) :cursors/hand-before-grab
     (ui/window-title-bar? actor)           :cursors/move-window
     (ui/button? actor)                     :cursors/over-button
     :else                               :cursors/default)))

(defn- player-effect-ctx [{:keys [cdq.context/mouseover-eid] :as c} eid]
  (let [target-position (or (and mouseover-eid
                                 (:position @mouseover-eid))
                            (c/world-mouse-position c))]
    {:effect/source eid
     :effect/target mouseover-eid
     :effect/target-position target-position
     :effect/target-direction (v/direction (:position @eid) target-position)}))

(defn- interaction-state [{:keys [cdq.context/mouseover-eid] :as c} eid]
  (let [entity @eid]
    (cond
     (c/mouse-on-actor? c)
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

  (state/manual-tick [[_ {:keys [eid]}] c]
    (if-let [movement-vector (player-movement-vector c)]
      (send-event! c eid :movement-input movement-vector)
      (let [[cursor on-click] (interaction-state c eid)]
        (c/set-cursor c cursor)
        (when (button-just-pressed? c :left)
          (on-click)))))

  (state/clicked-inventory-cell [[_ {:keys [eid player-idle/pickup-item-sound]}] cell c]
    ; TODO no else case
    (when-let [item (get-in (:entity/inventory @eid) cell)]
      (play pickup-item-sound)
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
      (play item-put-sound)
      (swap! eid dissoc :entity/item-on-cursor)
      (set-item c eid cell item-on-cursor)
      (send-event! c eid :dropped-item))

     ; STACK ITEMS
     (and item-in-cell
          (inventory/stackable? item-in-cell item-on-cursor))
     (do
      (play item-put-sound)
      (swap! eid dissoc :entity/item-on-cursor)
      (stack-item c eid cell item-on-cursor)
      (send-event! c eid :dropped-item))

     ; SWAP ITEMS
     (and item-in-cell
          (inventory/valid-slot? cell item-on-cursor))
     (do
      (play item-put-sound)
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
        (play place-world-item-sound)
        (swap! eid dissoc :entity/item-on-cursor)
        (spawn-item c
                    (item-place-position c entity)
                    (:entity/item-on-cursor entity)))))

  (state/manual-tick [[_ {:keys [eid]}] c]
    (when (and (button-just-pressed? c :left)
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

  (tick! [[_ {:keys [movement-vector]}] eid c]
    (if-let [movement-vector (player-movement-vector c)]
      (swap! eid assoc :entity/movement {:direction movement-vector
                                         :speed (entity/stat @eid :entity/movement-speed)})
      (send-event! c eid :no-movement-input))))

(defcomponent :stunned
  (state/cursor [_]
    :cursors/denied)

  (state/pause-game? [_]
    false)

  (tick! [[_ {:keys [counter]}] eid c]
    (when (timer/stopped? c counter)
      (send-event! c eid :effect-wears-off))))

(defcomponent :effects/audiovisual
  (effect/applicable? [_ {:keys [effect/target-position]}]
    target-position)

  (effect/useful? [_ _ _c]
    false)

  (effect/handle [[_ audiovisual] {:keys [effect/target-position]} c]
    (spawn-audiovisual c target-position audiovisual)))

(defn- projectile-start-point [entity direction size]
  (v/add (:position entity)
         (v/scale direction
                  (+ (:radius entity) size 0.1))))

(defcomponent :effects/projectile
  ; TODO for npcs need target -- anyway only with direction
  (effect/applicable? [_ {:keys [effect/target-direction]}]
    target-direction) ; faction @ source also ?

  ; TODO valid params direction has to be  non-nil (entities not los player ) ?
  (effect/useful? [[_ {:keys [projectile/max-range] :as projectile}]
                   {:keys [effect/source effect/target]}
                   c]
    (let [source-p (:position @source)
          target-p (:position @target)]
      ; is path blocked ereally needed? we need LOS also right to have a target-direction as AI?
      (and (not (path-blocked? c ; TODO test
                               source-p
                               target-p
                               (projectile-size projectile)))
           ; TODO not taking into account body sizes
           (< (v/distance source-p ; entity/distance function protocol EntityPosition
                          target-p)
              max-range))))

  (effect/handle [[_ projectile] {:keys [effect/source effect/target-direction]} c]
    (spawn-projectile c
                      {:position (projectile-start-point @source
                                                         target-direction
                                                         (projectile-size projectile))
                       :direction target-direction
                       :faction (:entity/faction @source)}
                      projectile)))

(comment
 ; mass shooting
 (for [direction (map math.vector/normalise
                      [[1 0]
                       [1 1]
                       [1 -1]
                       [0 1]
                       [0 -1]
                       [-1 -1]
                       [-1 1]
                       [-1 0]])]
   [:tx/projectile projectile-id ...]
   )
 )

(defcomponent :effects/sound
  (effect/applicable? [_ _ctx]
    true)

  (effect/useful? [_ _ _c]
    false)

  (effect/handle [[_ sound] _ctx c]
    (play sound)))

(defcomponent :effects/spawn
  (effect/applicable? [_ {:keys [effect/source effect/target-position]}]
    (and (:entity/faction @source)
         target-position))

  (effect/handle [[_ {:keys [property/id]}]
                  {:keys [effect/source effect/target-position]}
                  c]
    (spawn-creature c
                    {:position target-position
                     :creature-id id ; already properties/get called through one-to-one, now called again.
                     :components {:entity/fsm {:fsm :fsms/npc
                                               :initial-state :npc-idle}
                                  :entity/faction (:entity/faction @source)}})))

(comment
 ; TODO applicable targets? e.g. projectiles/effect s/???item entiteis ??? check
 ; same code as in render entities on world view screens/world
 ; TODO showing one a bit further up
 ; maybe world view port is cut
 ; not quite showing correctly.
 (let [targets (creatures-in-los-of-player)]
   (count targets)
   #_(sort-by #(% 1) (map #(vector (:entity.creature/name @%)
                                   (:position @%)) targets)))

 )
(defcomponent :effects/target-all
  ; TODO targets projectiles with -50% hp !!
  (effect/applicable? [_ _]
    true)

  (effect/useful? [_ _ _c]
    ; TODO
    false)

  (effect/handle [[_ {:keys [entity-effects]}] {:keys [effect/source]} c]
    (let [source* @source]
      (doseq [target (creatures-in-los-of-player c)]
        (line-render c
                     {:start (:position source*) #_(start-point source* target*)
                      :end (:position @target)
                      :duration 0.05
                      :color [1 0 0 0.75]
                      :thick? true})
        ; some sound .... or repeat smae sound???
        ; skill do sound  / skill start sound >?
        ; problem : nested tx/effect , we are still having direction/target-position
        ; at sub-effects
        ; and no more safe - merge
        ; find a way to pass ctx / effect-ctx separate ?
        (effect-ctx/do-all! c
                            {:effect/source source :effect/target target}
                            entity-effects)))))

(defcomponent :effects/target-entity
  (effect/applicable? [[_ {:keys [entity-effects]}] {:keys [effect/target] :as ctx}]
    (and target
         (seq (effect-ctx/filter-applicable? ctx entity-effects))))

  (effect/useful?  [[_ {:keys [maxrange]}] {:keys [effect/source effect/target]} _c]
    (entity/in-range? @source @target maxrange))

  (effect/handle [[_ {:keys [maxrange entity-effects]}] {:keys [effect/source effect/target] :as ctx} c]
    (let [source* @source
          target* @target]
      (if (entity/in-range? source* target* maxrange)
        (do
         (line-render c
                      {:start (entity/start-point source* target*)
                       :end (:position target*)
                       :duration 0.05
                       :color [1 0 0 0.75]
                       :thick? true})
         (effect-ctx/do-all! c ctx entity-effects))
        (spawn-audiovisual c
                           (entity/end-point source* target* maxrange)
                           (c/build c :audiovisuals/hit-ground))))))

(defcomponent :effects.target/audiovisual
  (effect/applicable? [_ {:keys [effect/target]}]
    target)

  (effect/useful? [_ _ _c]
    false)

  (effect/handle [[_ audiovisual] {:keys [effect/target]} c]
    (spawn-audiovisual c
                       (:position @target)
                       audiovisual)))

(defcomponent :effects.target/convert
  (effect/applicable? [_ {:keys [effect/source effect/target]}]
    (and target
         (= (:entity/faction @target)
            (entity/enemy @source))))

  (effect/handle [_ {:keys [effect/source effect/target]} c]
    (swap! target assoc :entity/faction (:entity/faction @source))))

(defn- effective-armor-save [source* target*]
  (max (- (or (entity/stat target* :entity/armor-save) 0)
          (or (entity/stat source* :entity/armor-pierce) 0))
       0))

(comment
 ; broken
 (let [source* {:entity/armor-pierce 0.4}
       target* {:entity/armor-save   0.5}]
   (effective-armor-save source* target*))
 )


(defn- armor-saves? [source* target*]
  (< (rand) (effective-armor-save source* target*)))

(defcomponent :effects.target/damage
  (effect/applicable? [_ {:keys [effect/target]}]
    (and target
         (:entity/hp @target)))

  (effect/handle [[_ damage] {:keys [effect/source effect/target]} c]
    (let [source* @source
          target* @target
          hp (entity/hitpoints target*)]
      (cond
       (zero? (hp 0))
       nil

       (armor-saves? source* target*)
       (swap! target add-text-effect c "[WHITE]ARMOR")

       :else
       (let [min-max (:damage/min-max (entity/damage source* target* damage))
             dmg-amount (rand-int-between min-max)
             new-hp-val (max (- (hp 0) dmg-amount) 0)]
         (swap! target assoc-in [:entity/hp 0] new-hp-val)
         (spawn-audiovisual c
                            (:position target*)
                            (c/build c :audiovisuals/damage))
         (send-event! c target (if (zero? new-hp-val) :kill :alert))
         (swap! target add-text-effect c (str "[RED]" dmg-amount "[]")))))))

(defcomponent :effects.target/kill
  (effect/applicable? [_ {:keys [effect/target]}]
    (and target
         (:entity/fsm @target)))

  (effect/handle [_ {:keys [effect/target]} c]
    (send-event! c target :kill)))

(defn- entity->melee-damage [entity]
  (let [strength (or (entity/stat entity :entity/strength) 0)]
    {:damage/min-max [strength strength]}))

(defn- melee-damage-effect [entity]
  [:effects.target/damage (entity->melee-damage entity)])

(defcomponent :effects.target/melee-damage
  (effect/applicable? [_ {:keys [effect/source] :as ctx}]
    (effect/applicable? (melee-damage-effect @source) ctx))

  (effect/handle [_ {:keys [effect/source] :as ctx} c]
    (effect/handle (melee-damage-effect @source) ctx c)))

(let [modifiers {:modifier/movement-speed {:op/mult -0.5}}
      duration 5]
  (defcomponent :effects.target/spiderweb
    (effect/applicable? [_ _]
      ; ?
      true)

    ; TODO stacking? (if already has k ?) or reset counter ? (see string-effect too)
    (effect/handle [_ {:keys [effect/target]} c]
      (when-not (:entity/temp-modifier @target)
        (swap! target assoc :entity/temp-modifier {:modifiers modifiers
                                                   :counter (timer/create c duration)})
        (swap! target entity/mod-add modifiers)))))

(defcomponent :effects.target/stun
  (effect/applicable? [_ {:keys [effect/target]}]
    (and target
         (:entity/fsm @target)))

  (effect/handle [[_ duration] {:keys [effect/target]} c]
    (send-event! c target :stun duration)))

(defn progress-time-if-not-paused [c]
  (if (:cdq.context/paused? c)
    c
    (-> c
        update-time
        tick-potential-fields
        tick-entities)))
