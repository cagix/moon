(ns cdq.world-fns.modules.print-grid
  (:require [cdq.grid2d.utils :as helper]))

(defn do! [{:keys [grid] :as world-fn-ctx}]
  (helper/printgrid grid)
  (println " - ")
  world-fn-ctx)
