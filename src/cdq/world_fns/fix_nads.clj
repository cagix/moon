(ns cdq.world-fns.fix-nads
  (:require [gdl.grid2d :as g2d]
            [gdl.grid2d.nads :as nads]))

(defn do!
  [{:keys [level/grid]
    :as level}]
  (assert (= #{:wall :ground} (set (g2d/cells grid))))
  (let [grid (nads/fix-nads grid)]
    (assert (= #{:wall :ground} (set (g2d/cells grid))))
    (assoc level :level/grid grid)))
