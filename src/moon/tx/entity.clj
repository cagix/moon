(ns ^:no-doc moon.tx.entity
  (:require [gdl.utils :refer [safe-merge dissoc-in]]
            [moon.component :as component]
            [moon.body :as body]
            [moon.entity :as entity]
            [moon.world.entities :as entities]))

(let [cnt (atom 0)]
  (defn- unique-number! []
    (swap! cnt inc)))

(defn- create-vs [components]
  (reduce (fn [m [k v]]
            (assoc m k (entity/->v [k v])))
          {}
          components))

(defmethod component/handle :e/create [[_ position body components]]
  (assert (and (not (contains? components :position))
               (not (contains? components :entity/id))))
  (let [eid (atom (-> body
                      (assoc :position position)
                      body/create
                      (safe-merge (-> components
                                      (assoc :entity/id (unique-number!))
                                      (create-vs)))))]
    (entities/add-to-world eid)
    (for [component @eid]
      #(entity/create component eid))))

(defmethod component/handle :tx/remove-destroyed-entities [_]
  (mapcat (fn [eid]
            (entities/remove-from-world eid)
            (for [component @eid]
              #(entity/destroy component eid)))
          (filter (comp :entity/destroyed? deref) (entities/all))))

(defmethod component/handle :e/assoc [[_ eid k v]]
  (assert (keyword? k))
  (swap! eid assoc k v)
  nil)

(defmethod component/handle :e/assoc-in [[_ eid ks v]]
  (swap! eid assoc-in ks v)
  nil)

(defmethod component/handle :e/dissoc [[_ eid k]]
  (assert (keyword? k))
  (swap! eid dissoc k)
  nil)

(defmethod component/handle :e/dissoc-in [[_ eid ks]]
  (swap! eid dissoc-in ks)
  nil)
