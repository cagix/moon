(ns cdq.world-fns.modules.create-initial-grid
  (:require [cdq.grid2d :as g2d]
            [cdq.grid2d.caves :as caves]
            [cdq.grid2d.nads :as nads]))

(defn- cave-grid [& {:keys [size]}]
  (let [{:keys [start grid]} (caves/create (java.util.Random.) size size :wide)
        grid (nads/fix-nads grid)]
    (assert (= #{:wall :ground} (set (g2d/cells grid))))
    {:start start
     :grid grid}))

(defn do!
  [{:keys [world/map-size]
    :as world-fn-ctx}]
  (let [{:keys [start grid]} (cave-grid :size map-size)]
    (assoc world-fn-ctx :start start :grid grid)))
