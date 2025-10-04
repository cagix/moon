(ns cdq.world-fns.fix-nads
  (:require [clojure.grid2d :as g2d]
            [clojure.grid2d.nads :as nads]))

(defn do!
  [{:keys [level/grid]
    :as level}]
  (assert (= #{:wall :ground} (set (g2d/cells grid))))
  (let [grid (nads/fix-nads grid)]
    (assert (= #{:wall :ground} (set (g2d/cells grid))))
    (assoc level :level/grid grid)))
