(ns cdq.entity.state.npc-dead)

(defn enter [_ eid]
  [[:tx/mark-destroyed eid]])
