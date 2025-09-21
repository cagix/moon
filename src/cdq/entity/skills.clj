(ns cdq.entity.skills)

(defn create! [skills eid _ctx]
  (cons [:tx/assoc eid :entity/skills nil]
        (for [skill skills]
          [:tx/add-skill eid skill])))
