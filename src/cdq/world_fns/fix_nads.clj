(ns cdq.world-fns.fix-nads
  (:require [cdq.grid2d :as g2d]
            [cdq.level.nads :as nads]))

(defn do!
  [{:keys [level/grid]
    :as level}]
  (assert (= #{:wall :ground} (set (g2d/cells grid))))
  (let [grid (nads/fix-nads grid)]
    (assert (= #{:wall :ground} (set (g2d/cells grid))))
    (assoc level :level/grid grid)))
