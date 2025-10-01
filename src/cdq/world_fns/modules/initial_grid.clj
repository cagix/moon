(ns cdq.world-fns.modules.initial-grid
  (:require [gdl.grid2d :as g2d]
            [gdl.grid2d.caves :as caves]
            [gdl.grid2d.nads :as nads]))

(defn- cave-grid [& {:keys [size]}]
  (let [{:keys [start grid]} (caves/create (java.util.Random.) size size :wide)
        grid (nads/fix-nads grid)]
    (assert (= #{:wall :ground} (set (g2d/cells grid))))
    {:start start
     :grid grid}))

(defn create
  [{:keys [world/map-size]
    :as world-fn-ctx}]
  (let [{:keys [start grid]} (cave-grid :size map-size)]
    (assoc world-fn-ctx
           :start start
           :grid grid)))
