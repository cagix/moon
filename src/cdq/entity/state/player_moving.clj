(ns cdq.entity.state.player-moving)

(defn create [eid movement-vector _ctx]
  {:movement-vector movement-vector})
