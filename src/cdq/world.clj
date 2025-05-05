(ns cdq.world
  (:require [cdq.audio.sound :as sound]
            [cdq.data.grid2d :as g2d]
            [cdq.db :as db]
            [cdq.entity :as entity]
            [cdq.graphics :as graphics]
            [cdq.grid :as grid]
            [cdq.math.vector2 :as v]
            [cdq.tiled :as tiled]
            [cdq.timer :as timer]
            [cdq.ui.stage :as stage]
            [cdq.utils :refer [define-order safe-merge tile->middle]]))

(defn- create-content-grid [{:keys [cell-size width height]}]
  {:grid (g2d/create-grid
          (inc (int (/ width  cell-size))) ; inc because corners
          (inc (int (/ height cell-size)))
          (fn [idx]
            (atom {:idx idx,
                   :entities #{}})))
   :cell-w cell-size
   :cell-h cell-size})

(defrecord RCell [position
                  middle ; only used @ potential-field-follow-to-enemy -> can remove it.
                  adjacent-cells
                  movement
                  entities
                  occupied
                  good
                  evil]
  grid/Cell
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
    (some #(not= % eid) occupied))

  (nearest-entity [this faction]
    (-> this faction :eid))

  (nearest-entity-distance [this faction]
    (-> this faction :distance)))

(defn- ->grid-cell [position movement]
  {:pre [(#{:none :air :all} movement)]}
  (map->RCell
   {:position position
    :middle (tile->middle position)
    :movement movement
    :entities #{}
    :occupied #{}}))

(defn- create-grid [tiled-map]
  (g2d/create-grid
   (tiled/tm-width tiled-map)
   (tiled/tm-height tiled-map)
   (fn [position]
     (atom (->grid-cell position
                        (case (tiled/movement-property tiled-map position)
                          "none" :none
                          "air"  :air
                          "all"  :all))))))

(defn- set-arr [arr cell cell->blocked?]
  (let [[x y] (:position cell)]
    (aset arr x y (boolean (cell->blocked? cell)))))

(defn- create-raycaster [grid]
  (let [width  (g2d/width  grid)
        height (g2d/height grid)
        arr (make-array Boolean/TYPE width height)]
    (doseq [cell (g2d/cells grid)]
      (set-arr arr @cell grid/blocks-vision?))
    [arr width height]))

(declare ^:private content-grid
         ^:private entity-ids
         explored-tile-corners
         grid
         raycaster)

(defn create! [tiled-map]
  (.bindRoot #'content-grid (create-content-grid {:cell-size 16
                                                  :width  (tiled/tm-width  tiled-map)
                                                  :height (tiled/tm-height tiled-map)}))
  (.bindRoot #'explored-tile-corners (atom (g2d/create-grid (tiled/tm-width  tiled-map)
                                                            (tiled/tm-height tiled-map)
                                                            (constantly false))))
  (.bindRoot #'entity-ids (atom {}))
  (.bindRoot #'grid (create-grid tiled-map))
  (.bindRoot #'raycaster (create-raycaster grid)))

(defn- active-entities* [{:keys [grid]} center-entity]
  (->> (let [idx (-> center-entity
                     :cdq.content-grid/content-cell
                     deref
                     :idx)]
         (cons idx (g2d/get-8-neighbour-positions idx)))
       (keep grid)
       (mapcat (comp :entities deref))))

(defn get-active-entities
  "Expensive operation.

  Active entities are those which are nearby the position of the player and about one screen away."
  [{:keys [cdq.context/player-eid]}]
  (active-entities* content-grid @player-eid))

(defn- set-cells! [grid eid]
  (let [cells (grid/rectangle->cells grid @eid)]
    (assert (not-any? nil? cells))
    (swap! eid assoc ::touched-cells cells)
    (doseq [cell cells]
      (assert (not (get (:entities @cell) eid)))
      (swap! cell update :entities conj eid))))

(defn- remove-from-cells! [eid]
  (doseq [cell (::touched-cells @eid)]
    (assert (get (:entities @cell) eid))
    (swap! cell update :entities disj eid)))

; could use inside tiles only for >1 tile bodies (for example size 4.5 use 4x4 tiles for occupied)
; => only now there are no >1 tile entities anyway
(defn- rectangle->occupied-cells [grid {:keys [left-bottom width height] :as rectangle}]
  (if (or (> (float width) 1) (> (float height) 1))
    (grid/rectangle->cells grid rectangle)
    [(grid [(int (+ (float (left-bottom 0)) (/ (float width) 2)))
            (int (+ (float (left-bottom 1)) (/ (float height) 2)))])]))

(defn- set-occupied-cells! [grid eid]
  (let [cells (rectangle->occupied-cells grid @eid)]
    (doseq [cell cells]
      (assert (not (get (:occupied @cell) eid)))
      (swap! cell update :occupied conj eid))
    (swap! eid assoc ::occupied-cells cells)))

(defn- remove-from-occupied-cells! [eid]
  (doseq [cell (::occupied-cells @eid)]
    (assert (get (:occupied @cell) eid))
    (swap! cell update :occupied disj eid)))

(defn- content-grid-update-entity! [content-grid eid]
  (let [{:keys [grid cell-w cell-h]} content-grid
        {:keys [cdq.content-grid/content-cell] :as entity} @eid
        [x y] (:position entity)
        new-cell (get grid [(int (/ x cell-w))
                            (int (/ y cell-h))])]
    (when-not (= content-cell new-cell)
      (swap! new-cell update :entities conj eid)
      (swap! eid assoc :cdq.content-grid/content-cell new-cell)
      (when content-cell
        (swap! content-cell update :entities disj eid)))))

(defn- add-entity! [eid]
  (let [id (:entity/id @eid)]
    (assert (number? id))
    (swap! entity-ids assoc id eid))

  (content-grid-update-entity! content-grid eid)

  ; https://github.com/damn/core/issues/58
  ;(assert (valid-position? grid @eid)) ; TODO deactivate because projectile no left-bottom remove that field or update properly for all
  (set-cells! grid eid)
  (when (:collides? @eid)
    (set-occupied-cells! grid eid)))

(defn- remove-entity! [eid]
  (let [id (:entity/id @eid)]
    (assert (contains? @entity-ids id))
    (swap! entity-ids dissoc id))

  (-> @eid
      :cdq.content-grid/content-cell
      (swap! update :entities disj eid))

  (remove-from-cells! eid)
  (when (:collides? @eid)
    (remove-from-occupied-cells! eid)))

(defn position-changed! [eid]
  (content-grid-update-entity! content-grid eid)

  (remove-from-cells! eid)
  (set-cells! grid eid)
  (when (:collides? @eid)
    (remove-from-occupied-cells! eid)
    (set-occupied-cells! grid eid)))

(defn remove-destroyed-entities! [context]
  (doseq [eid (filter (comp :entity/destroyed? deref)
                      (vals @entity-ids))]
    (remove-entity! eid)
    (doseq [component @eid]
      (entity/destroy! component eid context)))
  context)

; setting a min-size for colliding bodies so movement can set a max-speed for not
; skipping bodies at too fast movement
; TODO assert at properties load
(def minimum-size 0.39) ; == spider smallest creature size.

(def z-orders [:z-order/on-ground
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

(def id-counter (atom 0))

(defn- spawn-entity [context position body components]
  (assert (and (not (contains? components :position))
               (not (contains? components :entity/id))))
  (let [eid (atom (-> body
                      (assoc :position position)
                      create-body
                      (safe-merge (-> components
                                      (assoc :entity/id (swap! id-counter inc))
                                      (create-vs context)))))]
    (add-entity! eid)
    (doseq [component @eid]
      (entity/create! component eid context))
    eid))

(def ^{:doc "For effects just to have a mouseover body size for debugging purposes."
       :private true}
  effect-body-props
  {:width 0.5
   :height 0.5
   :z-order :z-order/effect})

(defn spawn-audiovisual [c position {:keys [tx/sound entity/animation]}]
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

(defn spawn-creature [c {:keys [position creature-id components]}]
  (let [props (db/build creature-id)]
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

(defn delayed-alert [c
                     position
                     faction
                     duration]
  (spawn-entity c
                position
                effect-body-props
                {:entity/alert-friendlies-after-duration
                 {:counter (timer/create duration)
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

(def ^:private shout-radius 4)

(defn friendlies-in-radius [grid position faction]
  (->> {:position position
        :radius shout-radius}
       (grid/circle->entities grid)
       (filter #(= (:entity/faction @%) faction))))

(defn nearest-enemy [entity]
  (grid/nearest-entity @(grid (entity/tile entity))
                       (entity/enemy entity)))

(defn world-item? []
  (not (stage/mouse-on-actor?)))

; It is possible to put items out of sight, losing them.
; Because line of sight checks center of entity only, not corners
; this is okay, you have thrown the item over a hill, thats possible.
(defn- placement-point [player target maxrange]
  (v/add player
         (v/scale (v/direction player target)
                  (min maxrange
                       (v/distance player target)))))

(defn item-place-position [_context entity]
  (placement-point (:position entity)
                   (graphics/world-mouse-position)
                   ; so you cannot put it out of your own reach
                   (- (:entity/click-distance-tiles entity) 0.1)))
