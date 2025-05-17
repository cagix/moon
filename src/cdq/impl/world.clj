(ns cdq.impl.world
  (:require [cdq.ctx :as ctx]
            [cdq.content-grid :as content-grid]
            [cdq.entity :as entity]
            [cdq.grid :as grid]
            [cdq.utils :as utils]
            [cdq.vector2 :as v]))

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

(defn spawn-entity! [position body components]
  (assert (and (not (contains? components :position))
               (not (contains? components :entity/id))))
  (let [eid (atom (-> body
                      (assoc :position position)
                      create-body
                      (utils/safe-merge (-> components
                                            (assoc :entity/id (swap! ctx/id-counter inc))
                                            create-vs))))]
    (let [id (:entity/id @eid)]
      (assert (number? id))
      (swap! ctx/entity-ids assoc id eid))
    (content-grid/add-entity! ctx/content-grid eid)
    ; https://github.com/damn/core/issues/58
    ;(assert (valid-position? grid @eid)) ; TODO deactivate because projectile no left-bottom remove that field or update properly for all
    (grid/add-entity! ctx/grid eid)
    (doseq [component @eid]
      (utils/handle-txs! (entity/create! component eid)))))
