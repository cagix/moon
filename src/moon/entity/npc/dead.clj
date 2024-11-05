(ns moon.entity.npc.dead)

(defn ->v [eid]
  {:eid eid})

(defn enter [{:keys [eid]}]
  [[:e/destroy eid]])
