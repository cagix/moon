(ns moon.entity.npc.dead)

(defn ->v [[_ eid]]
  {:eid eid})

(defn enter [[_ {:keys [eid]}]]
  [[:e/destroy eid]])
