(ns cdq.entity.stats)

(defn create [stats _world]
  (-> (if (:entity/mana stats)
        (update stats :entity/mana (fn [v] [v v]))
        stats)
      (update :entity/hp   (fn [v] [v v])))
  #_(-> stats
        (update :entity/mana (fn [v] [v v])) ; TODO is OPTIONAL ! then making [nil nil]
        (update :entity/hp   (fn [v] [v v]))))
