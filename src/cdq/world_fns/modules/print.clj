(ns cdq.world-fns.modules.print
  (:require [gdl.grid2d.utils :as helper]))

(defn do! [{:keys [grid] :as world-fn-ctx}]
  (helper/printgrid grid)
  (println " - ")
  world-fn-ctx)
