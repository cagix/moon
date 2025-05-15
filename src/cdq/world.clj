(ns cdq.world
  (:require [cdq.camera :as camera]
            [cdq.ctx :as ctx]
            [cdq.entity :as entity]
            [cdq.grid :as grid]
            [cdq.raycaster :as raycaster]
            [cdq.utils :as utils]
            [cdq.vector2 :as v]))

(defprotocol World
  (add-entity! [_ eid])
  (remove-entity! [_ eid])
  (position-changed! [_ eid])
  (cell [_ position]))

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

(def id-counter (atom 0))

(defn spawn-entity [position body components]
  (assert (and (not (contains? components :position))
               (not (contains? components :entity/id))))
  (let [eid (atom (-> body
                      (assoc :position position)
                      create-body
                      (utils/safe-merge (-> components
                                            (assoc :entity/id (swap! id-counter inc))
                                            create-vs))))]
    (add-entity! ctx/world eid)
    (doseq [component @eid]
      (utils/handle-txs! (entity/create! component eid)))))

(def ^{:doc "For effects just to have a mouseover body size for debugging purposes."}
  effect-body-props
  {:width 0.5
   :height 0.5
   :z-order :z-order/effect})

(defn projectile-size [projectile]
  {:pre [(:entity/image projectile)]}
  (first (:world-unit-dimensions (:entity/image projectile))))

(def ^:private shout-radius 4)

(defn friendlies-in-radius [grid position faction]
  (->> {:position position
        :radius shout-radius}
       (grid/circle->entities grid)
       (filter #(= (:entity/faction @%) faction))))

(defn nearest-enemy [{:keys [grid]} entity]
  (grid/nearest-entity @(grid (entity/tile entity))
                       (entity/enemy entity)))

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

; does not take into account size of entity ...
; => assert bodies <1 width then
(defn line-of-sight? [source target]
  (and (or (not (:entity/player? source))
           (on-screen? ctx/world-viewport target))
       (not (and los-checks?
                 (raycaster/blocked? (:raycaster ctx/world) (:position source) (:position target))))))

(defn creatures-in-los-of-player [{:keys [active-entities]}]
  (->> active-entities
       (filter #(:entity/species @%))
       (filter #(line-of-sight? @ctx/player-eid @%))
       (remove #(:entity/player? @%))))
