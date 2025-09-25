(ns cdq.world-fns.initial-grid-creation
  (:require [gdl.grid2d :as g2d]
            [gdl.grid2d.caves :as caves]))

(defn do!
  [{:keys [size
           cave-style
           random]
    :as level}]
  (let [{:keys [start grid]} (caves/create random size size cave-style)]
    (assert (= #{:wall :ground} (set (g2d/cells grid))))
    (assoc level
           :level/start start
           :level/grid grid)))
