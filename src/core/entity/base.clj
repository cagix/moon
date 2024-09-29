(in-ns 'core.entity)

(defn- define-order [order-k-vector]
  (apply hash-map (interleave order-k-vector (range))))

(defn- sort-by-order [coll get-item-order-k order]
  (sort-by #((get-item-order-k %) order) < coll))

#_(defn order-contains? [order k]
  ((apply hash-set (keys order)) k))

#_(deftest test-order
  (is
    (= (define-order [:a :b :c]) {:a 0 :b 1 :c 2}))
  (is
    (order-contains? (define-order [:a :b :c]) :a))
  (is
    (not
      (order-contains? (define-order [:a :b :c]) 2)))
  (is
    (=
      (sort-by-order [:c :b :a :b] identity (define-order [:a :b :c]))
      '(:a :b :b :c)))
  (is
    (=
      (sort-by-order [:b :c :null :null :a] identity (define-order [:c :b :a :null]))
      '(:c :b :a :null :null))))

;;;; ?

; java.lang.IllegalArgumentException: No method in multimethod 'render-info' for dispatch value: :position
; actually we dont want this to be called over that
; it should be :components? then ?
; => shouldn't need default fns for render -> don't call it if its not there

; every component has parent-entity-id (peid)
; fetch active entity-ids
; then fetch all components which implement render-below
; and have parent-id in entity-ids, etc.

;;;; Body

; so that at low fps the game doesn't jump faster between frames used @ movement to set a max speed so entities don't jump over other entities when checking collisions
(def max-delta-time 0.04)

; setting a min-size for colliding bodies so movement can set a max-speed for not
; skipping bodies at too fast movement
; TODO assert at properties load
(def ^:private min-solid-body-size 0.39) ; == spider smallest creature size.

; set max speed so small entities are not skipped by projectiles
; could set faster than max-speed if I just do multiple smaller movement steps in one frame
(def ^:private max-speed (/ min-solid-body-size max-delta-time)) ; need to make var because m/schema would fail later if divide / is inside the schema-form
(def movement-speed-schema (m/schema [:and number? [:>= 0] [:<= max-speed]]))

(def hpbar-height-px 5)

(def ^:private z-orders [:z-order/on-ground
                         :z-order/ground
                         :z-order/flying
                         :z-order/effect])

(def ^:private render-order (define-order z-orders))

(defrecord Entity [position
                   left-bottom
                   width
                   height
                   half-width
                   half-height
                   radius
                   collides?
                   z-order
                   rotation-angle])

(defn- ->Body [{[x y] :position
                :keys [position
                       width
                       height
                       collides?
                       z-order
                       rotation-angle]}]
  (assert position)
  (assert width)
  (assert height)
  (assert (>= width  (if collides? min-solid-body-size 0)))
  (assert (>= height (if collides? min-solid-body-size 0)))
  (assert (or (boolean? collides?) (nil? collides?)))
  (assert ((set z-orders) z-order))
  (assert (or (nil? rotation-angle)
              (<= 0 rotation-angle 360)))
  (map->Entity
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

(def ^{:doc "For effects just to have a mouseover body size for debugging purposes."}
  effect-body-props
  {:width 0.5
   :height 0.5
   :z-order :z-order/effect})

(defn entity-tile [entity*]
  (->tile (:position entity*)))

(defn direction [entity* other-entity*]
  (v-direction (:position entity*) (:position other-entity*)))

(defn collides? [entity* other-entity*]
  (shape-collides? entity* other-entity*))

;;;; ?

(defprotocol State
  (entity-state [_])
  (state-obj [_]))

(defprotocol Inventory
  (can-pickup-item? [_ item]))

(defprotocol Stats
  (entity-stat [_ stat] "Calculating value of the stat w. modifiers"))

(defprotocol Modifiers
  (->modified-value [_ modifier-k base-value]))

;;;; line-of-sight

; does not take into account zoom - but zoom is only for debug ???
; vision range?
(defn- on-screen? [entity* ctx]
  (let [[x y] (:position entity*)
        x (float x)
        y (float y)
        [cx cy] (camera-position (world-camera ctx))
        px (float cx)
        py (float cy)
        xdist (Math/abs (- x px))
        ydist (Math/abs (- y py))]
    (and
     (<= xdist (inc (/ (float (world-viewport-width ctx))  2)))
     (<= ydist (inc (/ (float (world-viewport-height ctx)) 2))))))

; TODO at wrong point , this affects targeting logic of npcs
; move the debug flag to either render or mouseover or lets see
(def ^:private ^:dbg-flag los-checks? true)

; does not take into account size of entity ...
; => assert bodies <1 width then
(defn line-of-sight? [context source* target*]
  (and (or (not (:entity/player? source*))
           (on-screen? target* context))
       (not (and los-checks?
                 (ray-blocked? context (:position source*) (:position target*))))))

(defsystem create "Create entity with eid for txs side-effects. Default nil."
  [_ entity ctx])
(defmethod create :default [_ entity ctx])

(defsystem destroy "FIXME" [_ entity ctx])
(defmethod destroy :default [_ entity ctx])

(defsystem tick "FIXME" [_ entity ctx])
(defmethod tick :default [_ entity ctx])

(defsystem render-below "FIXME" [_ entity* g ctx])
(defmethod render-below :default [_ entity* g ctx])

(defsystem render "FIXME" [_ entity* g ctx])
(defmethod render :default [_ entity* g ctx])

(defsystem render-above "FIXME" [_ entity* g ctx])
(defmethod render-above :default [_ entity* g ctx])

(defsystem render-info "FIXME" [_ entity* g ctx])
(defmethod render-info :default [_ entity* g ctx])

(def ^:private render-systems [render-below
                               render
                               render-above
                               render-info])

;;;; to world ctx?

(defcomponent :context/ecs
  (->mk [_ _ctx]
    {}))

(defn- entities [ctx] (:context/ecs ctx)) ; dangerous name!

(defn all-entities [ctx]
  (vals (entities ctx)))

(defn get-entity
  "Mostly used for debugging, use an entity's atom for (probably) faster access in your logic."
  [ctx uid]
  (get (entities ctx) uid))

(defcomponent :entity/id
  (create  [[_ id] _eid _ctx] [[:tx/add-to-world      id]])
  (destroy [[_ id] _eid _ctx] [[:tx/remove-from-world id]]))

(defcomponent :entity/uid
  {:let uid}
  (create [_ entity ctx]
    (assert (number? uid))
    (update ctx :context/ecs assoc uid entity))

  (destroy [_ _entity ctx]
    (assert (contains? (entities ctx) uid))
    (update ctx :context/ecs dissoc uid)))

(let [cnt (atom 0)]
  (defn- unique-number! []
    (swap! cnt inc)))

(defn- create-e-system [eid]
  (for [component @eid]
    (fn [ctx]
      ; we are assuming components dont remove other ones at entity/create
      ; thats why we reuse component and not fetch each time again for key
      (create component eid ctx))))

(defcomponent :e/create
  (do! [[_ position body components] ctx]
    (assert (and (not (contains? components :position))
                 (not (contains? components :entity/id))
                 (not (contains? components :entity/uid))))
    (let [eid (atom nil)]
      (reset! eid (-> body
                      (assoc :position position)
                      ->Body
                      (safe-merge (-> components
                                      (assoc :entity/id eid
                                             :entity/uid (unique-number!))
                                      (create-vs ctx)))))
      (create-e-system eid))))

(defcomponent :e/destroy
  (do! [[_ entity] ctx]
    [[:e/assoc entity :entity/destroyed? true]]))

(defcomponent :e/assoc
  (do! [[_ entity k v] ctx]
    (assert (keyword? k))
    (swap! entity assoc k v)
    ctx))

(defcomponent :e/assoc-in
  (do! [[_ entity ks v] ctx]
    (swap! entity assoc-in ks v)
    ctx))

(defcomponent :e/dissoc
  (do! [[_ entity k] ctx]
    (assert (keyword? k))
    (swap! entity dissoc k)
    ctx))

(defcomponent :e/dissoc-in
  (do! [[_ entity ks] ctx]
    (assert (> (count ks) 1))
    (swap! entity update-in (drop-last ks) dissoc (last ks))
    ctx))

(defcomponent :e/update-in
  (do! [[_ entity ks f] ctx]
    (swap! entity update-in ks f)
    ctx))


(def ^:private ^:dbg-flag show-body-bounds false)

(defn- draw-body-rect [g entity* color]
  (let [[x y] (:left-bottom entity*)]
    (draw-rectangle g x y (:width entity*) (:height entity*) color)))

(defn- render-entity* [system entity* g ctx]
  (try
   (when show-body-bounds
     (draw-body-rect g entity* (if (:collides? entity*) :white :gray)))
   (run! #(system % entity* g ctx) entity*)
   (catch Throwable t
     (draw-body-rect g entity* :red)
     (pretty-pst t 12))))

(defn- tick-system [ctx entity]
  (try
   (reduce (fn do-tick-component [ctx k]
             ; precaution in case a component gets removed by another component
             ; the question is do we still want to update nil components ?
             ; should be contains? check ?
             ; but then the 'order' is important? in such case dependent components
             ; should be moved together?
             (if-let [v (k @entity)]
               (let [component [k v]]
                 (effect! ctx (tick component entity ctx)))
               ctx))
           ctx
           (keys @entity))
   (catch Throwable t
     (throw (ex-info "" (select-keys @entity [:entity/uid]) t))
     ctx)))

(defn tick-entities!
  "Calls tick system on all components of entities."
  [ctx entities]
  (reduce tick-system ctx entities))

(defprotocol Player
  (player-entity [ctx])
  (player-entity* [ctx])
  (player-update-state      [ctx])
  (player-state-pause-game? [ctx])
  (player-clicked-inventory [ctx cell])
  (player-clicked-skillmenu [ctx skill]))

(defn render-entities!
  "Draws entities* in the correct z-order and in the order of render-systems for each z-order."
  [ctx g entities*]
  (let [player-entity* (player-entity* ctx)]
    (doseq [[z-order entities*] (sort-by-order (group-by :z-order entities*)
                                               first
                                               render-order)
            system render-systems
            entity* entities*
            :when (or (= z-order :z-order/effect)
                      (line-of-sight? ctx player-entity* entity*))]
      (render-entity* system entity* g ctx))))

(defn remove-destroyed-entities!
  "Calls destroy on all entities which are marked with ':e/destroy'"
  [ctx]
  (for [entity (filter (comp :entity/destroyed? deref) (all-entities ctx))
        component @entity]
    (fn [ctx]
      (destroy component entity ctx))))