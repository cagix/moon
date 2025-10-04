(ns cdq.entity.skills
  (:require [cdq.effect :as effect]
            [cdq.entity.stats :as stats]
            [cdq.entity.skills.skill :as skill]
            [clojure.timer :as timer]))

(extend-type clojure.lang.PersistentHashMap
  skill/Skill
  (usable-state [{:keys [skill/cooling-down? skill/effects] :as skill}
                 entity
                 effect-ctx]
    (cond
     cooling-down?
     :cooldown

     (stats/not-enough-mana? (:entity/stats entity) skill)
     :not-enough-mana

     (not (seq (filter #(effect/applicable? % effect-ctx) effects)))
     :invalid-params

     :else
     :usable)))

(defn create! [skills eid _world]
  (cons [:tx/assoc eid :entity/skills nil]
        (for [skill skills]
          [:tx/add-skill eid skill])))

(defn set-cooldown [skills skill elapsed-time]
  (assoc-in skills
            [(:property/id skill) :skill/cooling-down?]
            (timer/create elapsed-time (:skill/cooldown skill))))
