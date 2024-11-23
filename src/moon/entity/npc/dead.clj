(ns ^:no-doc moon.entity.npc.dead)

(defn ->v [eid]
  {:eid eid})

(defn enter [{:keys [eid]}]
  (swap! eid assoc :entity/destroyed? true))
