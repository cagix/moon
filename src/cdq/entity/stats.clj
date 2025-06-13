(ns cdq.entity.stats
  (:require [cdq.entity :as entity]
            [cdq.utils :refer [defmethods]]))

(defmethods :creature/stats
  (entity/create [[_ stats] _ctx]
    (-> (if (:entity/mana stats)
          (update stats :entity/mana (fn [v] [v v]))
          stats)
        (update :entity/hp   (fn [v] [v v])))
    #_(-> stats
        (update :entity/mana (fn [v] [v v])) ; TODO is OPTIONAL ! then making [nil nil]
        (update :entity/hp   (fn [v] [v v])))))
