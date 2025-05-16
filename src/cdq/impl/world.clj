(ns cdq.impl.world
  (:require [cdq.ctx :as ctx]
            [cdq.entity :as entity]
            [cdq.grid2d :as g2d]
            [cdq.utils :as utils]
            [cdq.vector2 :as v]
            [cdq.world :as world]
            [cdq.world.content-grid :as content-grid]
            [cdq.world.grid :as grid]
            [cdq.world.grid.cell :as cell]
            [cdq.world.raycaster :as raycaster]
            [cdq.world.potential-field :as potential-field]
            [gdl.graphics :as graphics]
            [gdl.graphics.camera :as camera]
            [gdl.tiled :as tiled]))

(def ^:private explored-tile-color (graphics/color 0.5 0.5 0.5 1))

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
            base-color (if explored? explored-tile-color graphics/black)
            cache-entry (get @light-cache position :not-found)
            blocked? (if (= cache-entry :not-found)
                       (let [blocked? (raycaster/blocked? raycaster light-position position)]
                         (swap! light-cache assoc position blocked?)
                         blocked?)
                       cache-entry)]
        #_(when @do-once
            (swap! ray-positions conj position))
        (if blocked?
          (if see-all-tiles? graphics/white base-color)
          (do (when-not explored?
                (swap! explored-tile-corners assoc (mapv int position) true))
              graphics/white))))))

; does not take into account zoom - but zoom is only for debug ???
; vision range?
(defn- on-screen? [viewport entity]
  (let [[x y] (:position entity)
        x (float x)
        y (float y)
        [cx cy] (camera/position (:camera viewport))
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

(defrecord RCell [position
                  middle ; only used @ potential-field-follow-to-enemy -> can remove it.
                  adjacent-cells
                  movement
                  entities
                  occupied
                  good
                  evil]
  cell/Cell
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
    :middle (utils/tile->middle position)
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

(defn- set-arr [arr cell cell->blocked?]
  (let [[x y] (:position cell)]
    (aset arr x y (boolean (cell->blocked? cell)))))

(defn- create-raycaster [grid]
  (let [width  (g2d/width  grid)
        height (g2d/height grid)
        arr (make-array Boolean/TYPE width height)]
    (doseq [cell (g2d/cells grid)]
      (set-arr arr @cell cell/blocks-vision?))
    [arr width height]))

(defn- add-entity! [{:keys [entity-ids content-grid grid]} eid]
  (let [id (:entity/id @eid)]
    (assert (number? id))
    (swap! entity-ids assoc id eid))
  (content-grid/add-entity! content-grid eid)
  ; https://github.com/damn/core/issues/58
  ;(assert (valid-position? grid @eid)) ; TODO deactivate because projectile no left-bottom remove that field or update properly for all
  (set-cells! grid eid)
  (when (:collides? @eid)
    (set-occupied-cells! grid eid)))

(defn- remove-entity! [{:keys [entity-ids]} eid]
  (let [id (:entity/id @eid)]
    (assert (contains? @entity-ids id))
    (swap! entity-ids dissoc id))
  (content-grid/remove-entity! eid)
  (remove-from-cells! eid)
  (when (:collides? @eid)
    (remove-from-occupied-cells! eid)))

(defrecord Body [position
                 left-bottom

                 width
                 height
                 half-width
                 half-height
                 radius

                 collides?
                 z-order
                 rotation-angle]
  entity/Entity
  (in-range? [entity target* maxrange] ; == circle-collides?
    (< (- (float (v/distance (:position entity)
                             (:position target*)))
          (float (:radius entity))
          (float (:radius target*)))
       (float maxrange))))

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
  (assert (>= width  (if collides? ctx/minimum-size 0)))
  (assert (>= height (if collides? ctx/minimum-size 0)))
  (assert (or (boolean? collides?) (nil? collides?)))
  (assert ((set ctx/z-orders) z-order))
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

(defn- create-vs [components]
  (reduce (fn [m [k v]]
            (assoc m k (entity/create [k v])))
          {}
          components))

(defrecord World [tiled-map
                  grid
                  raycaster
                  content-grid
                  explored-tile-corners
                  entity-ids
                  potential-field-cache
                  active-entities
                  id-counter]
  world/World
  (cache-active-entities [this]
    (assoc this :active-entities (content-grid/active-entities content-grid @ctx/player-eid)))

  (update-potential-fields! [_]
    (doseq [[faction max-iterations] ctx/factions-iterations]
      (potential-field/tick! potential-field-cache
                             grid
                             faction
                             active-entities
                             max-iterations)))

  (potential-field-direction [_ eid]
    (potential-field/find-direction grid eid))

  (draw-tiled-map! [_]
    (tiled/draw! (ctx/get-tiled-map-renderer tiled-map)
                 tiled-map
                 (tile-color-setter raycaster
                                    explored-tile-corners
                                    (camera/position (:camera ctx/world-viewport)))
                 (:camera ctx/world-viewport)))

  (remove-destroyed-entities! [this]
    (doseq [eid (filter (comp :entity/destroyed? deref) (vals @entity-ids))]
      (remove-entity! this eid)
      (doseq [component @eid]
        (utils/handle-txs! (entity/destroy! component eid)))))

  (spawn-entity! [this position body components]
    (assert (and (not (contains? components :position))
                 (not (contains? components :entity/id))))
    (let [eid (atom (-> body
                        (assoc :position position)
                        create-body
                        (utils/safe-merge (-> components
                                              (assoc :entity/id (swap! id-counter inc))
                                              create-vs))))]
      (add-entity! this eid)
      (doseq [component @eid]
        (utils/handle-txs! (entity/create! component eid)))))

  (position-changed! [_ eid]
    (content-grid/position-changed! content-grid eid)
    (remove-from-cells! eid)
    (set-cells! grid eid)
    (when (:collides? @eid)
      (remove-from-occupied-cells! eid)
      (set-occupied-cells! grid eid)))

  (cell [_ position]
    ; assert/document integer ?
    (grid position))


  ; does not take into account size of entity ...
  ; => assert bodies <1 width then
  (line-of-sight? [_ source target]
    (and (or (not (:entity/player? source))
             (on-screen? ctx/world-viewport target))
         (not (and los-checks?
                   (raycaster/blocked? raycaster
                                       (:position source)
                                       (:position target))))))

  (path-blocked? [_ start end width]
    (raycaster/path-blocked? raycaster ; TODO test
                             start
                             end
                             width)))

(defn create [tiled-map]
  (let [width  (tiled/tm-width  tiled-map)
        height (tiled/tm-height tiled-map)
        grid (create-grid tiled-map)]
    (map->World {:tiled-map tiled-map
                 :grid grid
                 :raycaster (create-raycaster grid)
                 :content-grid (content-grid/create {:cell-size 16
                                                     :width  width
                                                     :height height})
                 :explored-tile-corners (atom (g2d/create-grid width
                                                               height
                                                               (constantly false)))
                 :id-counter (atom 0)
                 :entity-ids (atom {})
                 :potential-field-cache (atom nil)
                 :active-entities nil})))
