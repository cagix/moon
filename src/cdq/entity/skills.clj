(ns cdq.entity.skills
  (:require [cdq.timer :as timer]
            [clojure.string :as str]))

(defn create! [skills eid _world]
  (cons [:tx/assoc eid :entity/skills nil]
        (for [skill skills]
          [:tx/add-skill eid skill])))

(defn tick [skills eid {:keys [world/elapsed-time]}]
  (for [{:keys [skill/cooling-down?] :as skill} (vals skills)
        :when (and cooling-down?
                   (timer/stopped? elapsed-time cooling-down?))]
    [:tx/assoc-in eid [:entity/skills (:property/id skill) :skill/cooling-down?] false]))

; TODO show cooldown?
(defn info-text [[_ skills] _world]
  ; => recursive info-text leads to endless text wall
  (when (seq skills)
    (str "Skills: " (str/join "," (map name (keys skills))))))
