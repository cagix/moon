(ns moon.entity.skills
  (:refer-clojure :exclude [remove])
  (:require [moon.system :refer [*k*]]
            [moon.entity :as entity]
            [moon.world :refer [stopped?]]))

(defn create [skills eid]
  (swap! eid assoc *k* nil)
  (doseq [skill skills]
    (swap! eid entity/add-skill skill)))

(defn info [skills]
  ; => recursive info-text leads to endless text wall
  #_(when (seq skills)
      (str "[VIOLET]Skills: " (str/join "," (map name (keys skills))) "[]")))

(defn tick [skills eid]
  (doseq [{:keys [skill/cooling-down?] :as skill} (vals skills)
          :when (and cooling-down?
                     (stopped? cooling-down?))]
    (swap! eid assoc-in [*k* (:property/id skill) :skill/cooling-down?] false)))
