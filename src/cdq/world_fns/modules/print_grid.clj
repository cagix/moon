(ns cdq.world-fns.modules.print-grid
  (:require [cdq.world-fns.helper :as helper]))

(defn do! [{:keys [grid] :as world-fn-ctx}]
  (helper/printgrid grid)
  (println " - ")
  world-fn-ctx)
