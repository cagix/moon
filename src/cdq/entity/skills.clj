(ns cdq.entity.skills
  (:require [cdq.entity :as entity]
            [cdq.g :as g]
            [gdl.utils :refer [defcomponent]]))

(defcomponent :entity/skills
  (entity/create! [[k skills] eid _ctx]
    (cons [:tx/assoc eid k nil]
          (for [skill skills]
            [:tx/add-skill eid skill])))

  (entity/tick! [[k skills] eid ctx]
    (for [{:keys [skill/cooling-down?] :as skill} (vals skills)
          :when (and cooling-down?
                     (g/timer-stopped? ctx cooling-down?))]
      [:tx/assoc-in eid [k (:property/id skill) :skill/cooling-down?] false])))
