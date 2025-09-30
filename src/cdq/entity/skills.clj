(ns cdq.entity.skills
  (:require [cdq.timer :as timer]))

(defn create! [skills eid _world]
  (cons [:tx/assoc eid :entity/skills nil]
        (for [skill skills]
          [:tx/add-skill eid skill])))
