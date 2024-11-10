(ns ^:no-doc moon.tx.entity
  (:require [gdl.utils :refer [safe-merge]]
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
