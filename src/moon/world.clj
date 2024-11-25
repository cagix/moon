(ns moon.world
  (:require [clj-commons.pretty.repl :refer [pretty-pst]]
            [data.grid2d :as g2d]
            [forge.app :refer [draw-rectangle world-camera world-viewport-width world-viewport-height]]
            [forge.assets :refer [play-sound]]
            [forge.db :as db]
            [forge.graphics.camera :as cam]
            [forge.math.vector :as v]
            [forge.utils :refer [dispose tile->middle define-order sort-by-order safe-merge]]
            [forge.tiled :as tiled]
            [forge.level :as level]
            [forge.world.raycaster :as raycaster :refer [ray-blocked?]]
            [malli.core :as m]
            [forge.entity :as entity]
            [moon.world.content-grid :as content-grid]
            [moon.world.grid :as grid]))

(declare tiled-map
         explored-tile-corners
         grid
         tick-error
         paused?
         ids->eids
         content-grid
         ^:private raycaster
         ^{:doc "The elapsed in-game-time in seconds (not counting when game is paused)."}
         elapsed-time
         ^{:doc "The game logic update delta-time. Different then forge.graphics/delta-time because it is bounded by a maximum value for entity movement speed."}
         delta
         player-eid)

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

(defn clear [] ; responsibility of screen? we are not creating the tiled-map here ...
  (when (bound? #'tiled-map)
    (dispose tiled-map)))

(defn cell [position]
  (get grid position))

(defn rectangle->cells        [rectangle] (grid/rectangle->cells        grid rectangle))
(defn circle->cells           [circle]    (grid/circle->cells           grid circle))
(defn circle->entities        [circle]    (grid/circle->entities        grid circle))
(defn cached-adjacent-cells   [cell]      (grid/cached-adjacent-cells   grid cell))
(defn point->entities         [position]  (grid/point->entities         grid position))
(def cells->entities grid/cells->entities)

(defprotocol GridCell
  (blocked? [cell* z-order])
  (blocks-vision? [cell*])
  (occupied-by-other? [cell* eid]
                      "returns true if there is some occupying body with center-tile = this cell
                      or a multiple-cell-size body which touches this cell.")
  (nearest-entity          [cell* faction])
  (nearest-entity-distance [cell* faction]))

(defrecord RCell [position
                  middle ; only used @ potential-field-follow-to-enemy -> can remove it.
                  adjacent-cells
                  movement
                  entities
                  occupied
                  good
                  evil]
  GridCell
  (blocked? [_ z-order]
    (case movement
      :none true ; wall
      :air (case z-order ; water/doodads
             :z-order/flying false
             :z-order/ground true)
      :all false)) ; ground/floor

  (blocks-vision? [_]
    (= movement :none))

  (occupied-by-other? [_ eid]
    (some #(not= % eid) occupied)) ; contains? faster?

  (nearest-entity [this faction]
    (-> this faction :eid))

  (nearest-entity-distance [this faction]
    (-> this faction :distance)))

(defn- ->cell [position movement]
  {:pre [(#{:none :air :all} movement)]}
  (map->RCell
   {:position position
    :middle (tile->middle position)
    :movement movement
    :entities #{}
    :occupied #{}}))

(def ^:private ^:dbg-flag show-body-bounds false)

(defn- draw-body-rect [entity color]
  (let [[x y] (:left-bottom entity)]
    (draw-rectangle x y (:width entity) (:height entity) color)))

(defn- render-entity! [system entity]
  (try
   (when show-body-bounds
     (draw-body-rect entity (if (:collides? entity) :white :gray)))
   (run! #(system % entity) entity)
   (catch Throwable t
     (draw-body-rect entity :red)
     (pretty-pst t 12))))

; precaution in case a component gets removed by another component
; the question is do we still want to update nil components ?
; should be contains? check ?
; but then the 'order' is important? in such case dependent components
; should be moved together?
(defn- tick-entity [eid]
  (try
   (doseq [k (keys @eid)]
     (try (when-let [v (k @eid)]
            (entity/tick [k v] eid))
          (catch Throwable t
            (throw (ex-info "entity/tick" {:k k} t)))))
   (catch Throwable t
     (throw (ex-info "" (select-keys @eid [:entity/id]) t)))))

(defn tick-entities [entities]
  (run! tick-entity entities))

(defn- add-to-world [eid]
  (let [id (:entity/id @eid)]
    (assert (number? id))
    (alter-var-root #'ids->eids assoc id eid))
  (content-grid/update-entity! content-grid eid)
  ; https://github.com/damn/core/issues/58
  ;(assert (valid-position? grid @eid)) ; TODO deactivate because projectile no left-bottom remove that field or update properly for all
  (grid/add-entity grid eid))

(defn- remove-from-world [eid]
  (let [id (:entity/id @eid)]
    (assert (contains? ids->eids id))
    (alter-var-root #'ids->eids dissoc id))
  (content-grid/remove-entity! eid)
  (grid/remove-entity eid))

(defn position-changed [eid]
  (content-grid/update-entity! content-grid eid)
  (grid/entity-position-changed grid eid))

(defn all-entities []
  (vals ids->eids))

(defn remove-destroyed []
  (doseq [eid (filter (comp :entity/destroyed? deref) (all-entities))]
    (remove-from-world eid)
    (doseq [component @eid]
      (entity/destroy component eid))))

(let [cnt (atom 0)]
  (defn- unique-number! []
    (swap! cnt inc)))

(defn- create-vs [components]
  (reduce (fn [m [k v]]
            (assoc m k (entity/->v [k v])))
          {}
          components))

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

; setting a min-size for colliding bodies so movement can set a max-speed for not
; skipping bodies at too fast movement
; TODO assert at properties load
(def minimum-body-size 0.39) ; == spider smallest creature size.

; so that at low fps the game doesn't jump faster between frames used @ movement to set a max speed so entities don't jump over other entities when checking collisions
(def max-delta-time 0.04)

; set max speed so small entities are not skipped by projectiles
; could set faster than max-speed if I just do multiple smaller movement steps in one frame
(def ^:private max-speed (/ minimum-body-size max-delta-time)) ; need to make var because m/schema would fail later if divide / is inside the schema-form
(def speed-schema (m/schema [:and number? [:>= 0] [:<= max-speed]]))


(def ^:private z-orders [:z-order/on-ground
                         :z-order/ground
                         :z-order/flying
                         :z-order/effect])

(def render-z-order (define-order z-orders))

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
  (assert (>= width  (if collides? minimum-body-size 0)))
  (assert (>= height (if collides? minimum-body-size 0)))
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

(defn- create [position body components]
  (assert (and (not (contains? components :position))
               (not (contains? components :entity/id))))
  (let [eid (atom (-> body
                      (assoc :position position)
                      create-body
                      (safe-merge (-> components
                                      (assoc :entity/id (unique-number!))
                                      (create-vs)))))]
    (add-to-world eid)
    (doseq [component @eid]
      (entity/create component eid))
    eid))

(def ^{:doc "For effects just to have a mouseover body size for debugging purposes."
       :private true}
  effect-body-props
  {:width 0.5
   :height 0.5
   :z-order :z-order/effect})

(defn audiovisual [position {:keys [tx/sound entity/animation]}]
  (play-sound sound)
  (create position
          effect-body-props
          {:entity/animation animation
           :entity/delete-after-animation-stopped true}))

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

(defn creature [{:keys [position creature-id components]}]
  (let [props (db/get creature-id)]
    (create position
            (->body (:entity/body props))
            (-> props
                (dissoc :entity/body)
                (assoc :entity/destroy-audiovisual :audiovisuals/creature-die)
                (safe-merge components)))))

(defn item [position item]
  (create position
          {:width 0.75
           :height 0.75
           :z-order :z-order/on-ground}
          {:entity/image (:entity/image item)
           :entity/item item
           :entity/clickable {:type :clickable/item
                              :text (:property/pretty-name item)}}))

(defn shout [position faction duration]
  (create position
          effect-body-props
          {:entity/alert-friendlies-after-duration
           {:counter (timer duration)
            :faction faction}}))

(defn line-render [{:keys [start end duration color thick?]}]
  (create start
          effect-body-props
          #:entity {:line-render {:thick? thick? :end end :color color}
                    :delete-after-duration duration}))

(defn projectile-size [projectile]
  {:pre [(:entity/image projectile)]}
  (first (:world-unit-dimensions (:entity/image projectile))))

(defn projectile [{:keys [position direction faction]}
                  {:keys [entity/image
                          projectile/max-range
                          projectile/speed
                          entity-effects
                          projectile/piercing?] :as projectile}]
  (let [size (projectile-size projectile)]
    (create position
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

(def ^:private ^:dbg-flag spawn-enemies? true)

; player-creature needs mana & inventory
; till then hardcode :creatures/vampire
(defn- spawn-player [start-position]
  (creature {:position (tile->middle start-position)
             :creature-id :creatures/vampire
             :components {:entity/fsm {:fsm :fsms/player
                                       :initial-state :player-idle}
                          :entity/faction :good
                          :entity/player? true
                          :entity/free-skill-points 3
                          :entity/clickable {:type :clickable/player}
                          :entity/click-distance-tiles 1.5}}))

(defn- spawn-enemies [tiled-map]
  (doseq [props (for [[position creature-id] (tiled/positions-with-property tiled-map :creatures :id)]
                  {:position position
                   :creature-id (keyword creature-id)
                   :components {:entity/fsm {:fsm :fsms/npc
                                             :initial-state :npc-sleeping}
                                :entity/faction :evil}})]
    (creature (update props :position tile->middle))))

(defn init [{:keys [tiled-map start-position]}]
  (.bindRoot #'tiled-map tiled-map)
  (.bindRoot #'explored-tile-corners (atom (g2d/create-grid (tiled/width  tiled-map)
                                                            (tiled/height tiled-map)
                                                            (constantly false))))
  (.bindRoot #'grid (g2d/create-grid
                     (tiled/width tiled-map)
                     (tiled/height tiled-map)
                     (fn [position]
                       (atom (->cell position
                                     (case (level/movement-property tiled-map position)
                                       "none" :none
                                       "air"  :air
                                       "all"  :all))))))

  (raycaster/init grid blocks-vision?)
  (let [width  (tiled/width  tiled-map)
        height (tiled/height tiled-map)]
    (.bindRoot #'content-grid (content-grid/create {:cell-size 16  ; FIXME global config
                                                    :width  width
                                                    :height height})))
  (.bindRoot #'tick-error nil)
  (.bindRoot #'ids->eids {})
  (.bindRoot #'elapsed-time 0)
  (.bindRoot #'delta nil)
  (.bindRoot #'player-eid (spawn-player start-position))
  (when spawn-enemies?
    (spawn-enemies tiled-map)))

(defn active-entities []
  (content-grid/active-entities content-grid @player-eid))

; does not take into account zoom - but zoom is only for debug ???
; vision range?
(defn- on-screen? [entity]
  (let [[x y] (:position entity)
        x (float x)
        y (float y)
        [cx cy] (cam/position (world-camera))
        px (float cx)
        py (float cy)
        xdist (Math/abs (- x px))
        ydist (Math/abs (- y py))]
    (and
     (<= xdist (inc (/ (float (world-viewport-width))  2)))
     (<= ydist (inc (/ (float (world-viewport-height)) 2))))))

; TODO at wrong point , this affects targeting logic of npcs
; move the debug flag to either render or mouseover or lets see
(def ^:private ^:dbg-flag los-checks? true)

; does not take into account size of entity ...
; => assert bodies <1 width then
(defn line-of-sight? [source target]
  (and (or (not (:entity/player? source))
           (on-screen? target))
       (not (and los-checks?
                 (ray-blocked? (:position source) (:position target))))))

(defn render-entities
  "Draws entities in the correct z-order and in the order of render-systems for each z-order."
  [entities]
  (let [player @player-eid]
    (doseq [[z-order entities] (sort-by-order (group-by :z-order entities)
                                              first
                                              render-z-order)
            system [entity/render-below
                    entity/render
                    entity/render-above
                    entity/render-info]
            entity entities
            :when (or (= z-order :z-order/effect)
                      (line-of-sight? player entity))]
      (render-entity! system entity))))
