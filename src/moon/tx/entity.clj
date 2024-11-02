(ns moon.tx.entity
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

(defmethods :e/create
  (component/handle [[_ position body components]]
    (assert (and (not (contains? components :position))
                 (not (contains? components :entity/id))))
    (let [eid (atom (-> body
                        (assoc :position position)
                        body/create
                        (safe-merge (-> components
                                        (assoc :entity/id (unique-number!))
                                        (create-vs)))))]
      (cons [:tx/add-to-world eid]
            (for [component @eid]
              #(entity/create component eid))))))

(defmethods :e/destroy
  (component/handle [[_ eid]]
    [[:e/assoc eid :entity/destroyed? true]]))

(defmethods :tx/remove-destroyed-entities
  (component/handle [_]
    (mapcat (fn [eid]
              (cons [:tx/remove-from-world eid]
                    (for [component @eid]
                      #(entity/destroy component eid))))
            (filter (comp :entity/destroyed? deref) (entities/all)))))

(defmethods :e/assoc
  (component/handle [[_ eid k v]]
    (assert (keyword? k))
    (swap! eid assoc k v)
    nil))

(defmethods :e/assoc-in
  (component/handle [[_ eid ks v]]
    (swap! eid assoc-in ks v)
    nil))

(defmethods :e/dissoc
  (component/handle [[_ eid k]]
    (assert (keyword? k))
    (swap! eid dissoc k)
    nil))

(defmethods :e/dissoc-in
  (component/handle [[_ eid ks]]
    (swap! eid dissoc-in ks)
    nil))
