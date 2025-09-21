(ns cdq.entity.projectile-collision)

(defn create [v _ctx]
  (assoc v :already-hit-bodies #{}))
