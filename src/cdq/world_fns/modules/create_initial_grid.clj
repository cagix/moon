(ns cdq.world-fns.modules.create-initial-grid
  (:require [cdq.world-fns.helper :as helper]))

(defn do!
  [{:keys [world/map-size]
    :as world-fn-ctx}]
  (let [{:keys [start grid]} (helper/cave-grid :size map-size)]
    (assoc world-fn-ctx :start start :grid grid)))
