(ns forge.world
  (:require [data.grid2d :as g2d]
            [forge.graphics.camera :as cam]
            [forge.level :as level]
            [forge.tiled :as tiled]
            [malli.core :as m]
            [forge.entity :as entity]
            [forge.world.grid :as grid])
  (:import (forge RayCaster)))

(defn- content-grid-create [{:keys [cell-size width height]}]
  {:grid (grid2d (inc (int (/ width  cell-size))) ; inc because corners
                 (inc (int (/ height cell-size)))
                 (fn [idx]
                   (atom {:idx idx,
                          :entities #{}})))
   :cell-w cell-size
   :cell-h cell-size})

(defn- content-grid-update-entity! [{:keys [grid cell-w cell-h]} eid]
  (let [{::keys [content-cell] :as entity} @eid
        [x y] (:position entity)
        new-cell (get grid [(int (/ x cell-w))
                            (int (/ y cell-h))])]
    (when-not (= content-cell new-cell)
      (swap! new-cell update :entities conj eid)
      (swap! eid assoc ::content-cell new-cell)
      (when content-cell
        (swap! content-cell update :entities disj eid)))))

(defn- content-grid-remove-entity! [eid]
  (-> @eid
      ::content-cell
      (swap! update :entities disj eid)))

(defn- content-grid-active-entities [{:keys [grid]} center-entity]
  (->> (let [idx (-> center-entity
                     ::content-cell
                     deref
                     :idx)]
         (cons idx (get-8-neighbour-positions idx)))
       (keep grid)
       (mapcat (comp :entities deref))))

(declare tiled-map
         explored-tile-corners
         grid
         tick-error
         paused?
         ids->eids
         content-grid
         ^:private ray-caster
         ^{:doc "The elapsed in-game-time in seconds (not counting when game is paused)."}
         elapsed-time
         ^{:doc "The game logic update delta-time. Different then forge.graphics/delta-time because it is bounded by a maximum value for entity movement speed."}
         delta
         player-eid)

; boolean array used because 10x faster than access to clojure grid data structure

; this was a serious performance bottleneck -> alength is counting the whole array?
;(def ^:private width alength)
;(def ^:private height (comp alength first))

; does not show warning on reflection, but shows cast-double a lot.
(defn- rc-blocked? [[arr width height] [start-x start-y] [target-x target-y]]
  (RayCaster/rayBlocked (double start-x)
                        (double start-y)
                        (double target-x)
                        (double target-y)
                        width ;(width boolean-2d-array)
                        height ;(height boolean-2d-array)
                        arr))

#_(defn ray-steplist [boolean-2d-array [start-x start-y] [target-x target-y]]
  (seq
   (RayCaster/castSteplist start-x
                           start-y
                           target-x
                           target-y
                           (width boolean-2d-array)
                           (height boolean-2d-array)
                           boolean-2d-array)))

#_(defn ray-maxsteps [boolean-2d-array [start-x start-y] [vector-x vector-y] max-steps]
  (let [erg (RayCaster/castMaxSteps start-x
                                    start-y
                                    vector-x
                                    vector-y
                                    (width boolean-2d-array)
                                    (height boolean-2d-array)
                                    boolean-2d-array
                                    max-steps
                                    max-steps)]
    (if (= -1 erg)
      :not-blocked
      erg)))

; STEPLIST TEST

#_(def current-steplist (atom nil))

#_(defn steplist-contains? [tilex tiley] ; use vector equality
  (some
    (fn [[x y]]
      (and (= x tilex) (= y tiley)))
    @current-steplist))

#_(defn render-line-middle-to-mouse [color]
  (let [[x y] (input/get-mouse-pos)]
    (g/draw-line (/ (g/viewport-width) 2)
                 (/ (g/viewport-height) 2)
                 x y color)))

#_(defn update-test-raycast-steplist []
    (reset! current-steplist
            (map
             (fn [step]
               [(.x step) (.y step)])
             (raycaster/ray-steplist (get-cell-blocked-boolean-array)
                                     (:position @world-player)
                                     (g/map-coords)))))

;; MAXSTEPS TEST

#_(def current-steps (atom nil))

#_(defn update-test-raycast-maxsteps []
    (let [maxsteps 10]
      (reset! current-steps
              (raycaster/ray-maxsteps (get-cell-blocked-boolean-array)
                                      (v-direction (g/map-coords) start)
                                      maxsteps))))

#_(defn draw-test-raycast []
  (let [start (:position @world-player)
        target (g/map-coords)
        color (if (fast-ray-blocked? start target) g/red g/green)]
    (render-line-middle-to-mouse color)))

; PATH BLOCKED TEST

#_(defn draw-test-path-blocked [] ; TODO draw in map no need for screenpos-of-tilepos
  (let [[start-x start-y] (:position @world-player)
        [target-x target-y] (g/map-coords)
        [start1 target1 start2 target2] (create-double-ray-endpositions start-x start-y target-x target-y 0.4)
        [start1screenx,start1screeny]   (screenpos-of-tilepos start1)
        [target1screenx,target1screeny] (screenpos-of-tilepos target1)
        [start2screenx,start2screeny]   (screenpos-of-tilepos start2)
        [target2screenx,target2screeny] (screenpos-of-tilepos target2)
        color (if (is-path-blocked? start1 target1 start2 target2)
                g/red
                g/green)]
    (g/draw-line start1screenx start1screeny target1screenx target1screeny color)
    (g/draw-line start2screenx start2screeny target2screenx target2screeny color)))

; TO math.... // not tested
(defn- create-double-ray-endpositions
  "path-w in tiles."
  [[start-x start-y] [target-x target-y] path-w]
  {:pre [(< path-w 0.98)]} ; wieso 0.98??
  (let [path-w (+ path-w 0.02) ;etwas gr�sser damit z.b. projektil nicht an ecken anst�sst
        v (v-direction [start-x start-y]
                       [target-y target-y])
        [normal1 normal2] (v-normal-vectors v)
        normal1 (v-scale normal1 (/ path-w 2))
        normal2 (v-scale normal2 (/ path-w 2))
        start1  (v-add [start-x  start-y]  normal1)
        start2  (v-add [start-x  start-y]  normal2)
        target1 (v-add [target-x target-y] normal1)
        target2 (v-add [target-x target-y] normal2)]
    [start1,target1,start2,target2]))

(defn- path-blocked?*
  "path-w in tiles. casts two rays."
  [raycaster start target path-w]
  (let [[start1,target1,start2,target2] (create-double-ray-endpositions start target path-w)]
    (or
     (rc-blocked? raycaster start1 target1)
     (rc-blocked? raycaster start2 target2))))

(defn- set-arr [arr cell cell->blocked?]
  (let [[x y] (:position cell)]
    (aset arr x y (boolean (cell->blocked? cell)))))

(defn- init-raycaster [grid position->blocked?]
  (let [width  (g2d/width  grid)
        height (g2d/height grid)
        arr (make-array Boolean/TYPE width height)]
    (doseq [cell (g2d/cells grid)]
      (set-arr arr @cell position->blocked?))
    (bind-root #'ray-caster [arr width height])))

(defn ray-blocked? [start target]
  (rc-blocked? ray-caster start target))

(defn path-blocked?
  "path-w in tiles. casts two rays."
  [start target path-w]
  (path-blocked?* ray-caster start target path-w))

(def mouseover-eid nil)

(defn mouseover-entity []
  (and mouseover-eid
       @mouseover-eid))

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
     (draw-body-rect entity (if (:collides? entity) white :gray)))
   (run! #(system % entity) entity)
   (catch Throwable t
     (draw-body-rect entity :red)
     (pretty-pst t))))

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
  (content-grid-update-entity! content-grid eid)
  ; https://github.com/damn/core/issues/58
  ;(assert (valid-position? grid @eid)) ; TODO deactivate because projectile no left-bottom remove that field or update properly for all
  (grid/add-entity grid eid))

(defn- remove-from-world [eid]
  (let [id (:entity/id @eid)]
    (assert (contains? ids->eids id))
    (alter-var-root #'ids->eids dissoc id))
  (content-grid-remove-entity! eid)
  (grid/remove-entity eid))

(defn position-changed [eid]
  (content-grid-update-entity! content-grid eid)
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
  (let [props (build creature-id)]
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
             :rotation-angle (v-angle-from-vector direction)}
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
  (bind-root #'tiled-map tiled-map)
  (bind-root #'explored-tile-corners (atom (grid2d (tiled/width  tiled-map)
                                                   (tiled/height tiled-map)
                                                   (constantly false))))
  (bind-root #'grid (grid2d (tiled/width tiled-map)
                            (tiled/height tiled-map)
                            (fn [position]
                              (atom (->cell position
                                            (case (level/movement-property tiled-map position)
                                              "none" :none
                                              "air"  :air
                                              "all"  :all))))))

  (init-raycaster grid blocks-vision?)
  (let [width  (tiled/width  tiled-map)
        height (tiled/height tiled-map)]
    (bind-root #'content-grid (content-grid-create {:cell-size 16  ; FIXME global config
                                                    :width  width
                                                    :height height})))
  (bind-root #'tick-error nil)
  (bind-root #'ids->eids {})
  (bind-root #'elapsed-time 0)
  (bind-root #'delta nil)
  (bind-root #'player-eid (spawn-player start-position))
  (when spawn-enemies?
    (spawn-enemies tiled-map)))

(defn active-entities []
  (content-grid-active-entities content-grid @player-eid))

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
     (<= xdist (inc (/ (float world-viewport-width)  2)))
     (<= ydist (inc (/ (float world-viewport-height) 2))))))

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
