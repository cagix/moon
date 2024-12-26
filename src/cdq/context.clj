(ns cdq.context
  (:require [anvil.component :as component]
            [anvil.entity :as entity]
            [anvil.world.content-grid :as content-grid]
            [cdq.grid :as grid]
            [clojure.gdx.audio.sound :as sound]
            [gdl.context :as c]
            [gdl.graphics.camera :as cam]
            [gdl.math.raycaster :as raycaster]
            [gdl.math.vector :as v]))

(defn widgets [c])

(defn create [c world-id])
(defn dispose [w])
(defn render [])
(defn tick [])

(declare ^:private tiled-map
         ^:private explored-tile-corners
         ^:private grid
         ^:private entity-ids
         ^:private content-grid
         ^:private player-eid
         ^:private raycaster

         ^{:doc "The elapsed in-game-time in seconds (not counting when game is paused)."}
         elapsed-time

         ^{:doc "The game logic update delta-time in ms."}
         delta-time

         paused?
         mouseover-eid
         error)

(defn state []
  {::grid grid
   ::factions-iterations {:good 15 :evil 5}
   ::tiled-map tiled-map
   ::player-eid player-eid
   ::explored-tile-corners explored-tile-corners
   ::entity-ids entity-ids
   ::content-grid content-grid
   ::raycaster raycaster
   })

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

(defn timer [duration]
  {:pre [(>= duration 0)]}
  {:duration duration
   :stop-time (+ elapsed-time duration)})

(defn stopped? [{:keys [stop-time]}]
  (>= elapsed-time stop-time))

(defn reset-timer [{:keys [duration] :as counter}]
  (assoc counter :stop-time (+ elapsed-time duration)))

(defn finished-ratio [{:keys [duration stop-time] :as counter}]
  {:post [(<= 0 % 1)]}
  (if (stopped? counter)
    0
    ; min 1 because floating point math inaccuracies
    (min 1 (/ (- stop-time elapsed-time) duration))))

(defn mouseover-entity []
  (and mouseover-eid
       @mouseover-eid))

(defn all-entities [{::keys [entity-ids]}]
  (vals entity-ids))

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
(defn add-text-effect [entity text]
  (assoc entity
         :entity/string-effect
         (if-let [string-effect (:entity/string-effect entity)]
           (-> string-effect
               (update :text str "\n" text)
               (update :counter reset-timer))
           {:text text
            :counter (timer 0.4)})))

(defn- entity-ids-add-entity [eid]
  (let [id (:entity/id @eid)]
    (assert (number? id))
    (alter-var-root #'entity-ids assoc id eid)))

(defn- entity-ids-remove-entity [eid]
  (let [id (:entity/id @eid)]
    (assert (contains? entity-ids id))
    (alter-var-root #'entity-ids dissoc id)))

(defn active-entities [{::keys [content-grid player-eid]}]
  (content-grid/active-entities content-grid @player-eid))

(defn- add-entity [eid]
  ; https://github.com/damn/core/issues/58
  ;(assert (valid-position? grid @eid)) ; TODO deactivate because projectile no left-bottom remove that field or update properly for all
  (content-grid/add-entity content-grid eid)
  (entity-ids-add-entity   eid)
  (grid/add-entity grid eid))

(defn- remove-entity [eid]
  (content-grid/remove-entity eid)
  (entity-ids-remove-entity   eid)
  (grid/remove-entity         eid))

(defn position-changed [eid]
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

(defn- create-vs [components c]
  (reduce (fn [m [k v]]
            (assoc m k (component/->v [k v] c)))
          {}
          components))

(defn spawn-entity [c position body components]
  (assert (and (not (contains? components :position))
               (not (contains? components :entity/id))))
  (let [eid (atom (-> body
                      (assoc :position position)
                      create-body
                      (safe-merge (-> components
                                      (assoc :entity/id (unique-number!))
                                      (create-vs c)))))]
    (add-entity eid)
    (doseq [component @eid]
      (component/create component eid c))
    eid))

(def ^{:doc "For effects just to have a mouseover body size for debugging purposes."
       :private true}
  effect-body-props
  {:width 0.5
   :height 0.5
   :z-order :z-order/effect})

(defn audiovisual [c position {:keys [tx/sound entity/animation]}]
  (sound/play sound)
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

(defn creature [c {:keys [position creature-id components]}]
  (let [props (c/build c creature-id)]
    (spawn-entity c
                  position
                  (->body (:entity/body props))
                  (-> props
                      (dissoc :entity/body)
                      (assoc :entity/destroy-audiovisual :audiovisuals/creature-die)
                      (safe-merge components)))))

(defn item [c position item]
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
                 {:counter (timer duration)
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

(defn projectile [c
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

(defn remove-destroyed-entities [c]
  (doseq [eid (filter (comp :entity/destroyed? deref)
                      (all-entities c))]
    (remove-entity eid)
    (doseq [component @eid]
      (component/destroy component eid c))))

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

(defn nearest-enemy [entity]
  (grid/nearest-entity @(grid (entity/tile entity))
                       (entity/enemy entity)))
