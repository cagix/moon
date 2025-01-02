(ns cdq.entity.state.npc-dead)

(defn create [[_ eid] c]
  {:eid eid})

(defn enter [[_ {:keys [eid]}] c]
  (swap! eid assoc :entity/destroyed? true))
