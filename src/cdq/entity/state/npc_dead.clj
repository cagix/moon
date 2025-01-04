(ns cdq.entity.state.npc-dead)

(defn enter [[_ {:keys [eid]}] c]
  (swap! eid assoc :entity/destroyed? true))
