(ns forge.entity.state.npc-dead)

(defn ->v [[_ eid]]
  {:eid eid})

(defn enter [[_ {:keys [eid]}]]
  (swap! eid assoc :entity/destroyed? true))
