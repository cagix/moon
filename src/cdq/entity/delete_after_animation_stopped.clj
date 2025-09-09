(ns cdq.entity.delete-after-animation-stopped)

(defn create! [_ eid _ctx]
  (-> @eid :entity/animation :looping? not assert)
  nil)
