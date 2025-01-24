(ns cdq.impl.world.spawn-entity
  (:require [cdq.context :as context]
            [cdq.entity :as entity]
            [cdq.fsm :as fsm]
            [cdq.graphics.animation :as animation]
            [cdq.inventory :as inventory]
            [cdq.widgets.inventory :as widgets.inventory]
            [cdq.world :as world]
            [clojure.utils :refer [safe-merge]]))

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
  (assert (>= width  (if collides? world/minimum-size 0)))
  (assert (>= height (if collides? world/minimum-size 0)))
  (assert (or (boolean? collides?) (nil? collides?)))
  (assert ((set world/z-orders) z-order))
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

(defmulti create! (fn [[k] eid c]
                    k))
(defmethod create! :default [_ eid c])

(def id-counter (atom 0))

(extend-type cdq.context.Context
  world/World
  (spawn-entity [context position body components]
    (assert (and (not (contains? components :position))
                 (not (contains? components :entity/id))))
    (let [eid (atom (-> body
                        (assoc :position position)
                        create-body
                        (safe-merge (-> components
                                        (assoc :entity/id (swap! id-counter inc))
                                        (create-vs context)))))]
      (doseq [component context]
        (context/add-entity component eid))
      (doseq [component @eid]
        (create! component eid context))
      eid)))

(defmethod create! :entity/inventory
  [[k items] eid c]
  (swap! eid assoc k inventory/empty-inventory)
  (doseq [item items]
    (widgets.inventory/pickup-item c eid item)))

(defmethod create! :entity/skills
  [[k skills] eid c]
  (swap! eid assoc k nil)
  (doseq [skill skills]
    (world/add-skill c eid skill)))

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
