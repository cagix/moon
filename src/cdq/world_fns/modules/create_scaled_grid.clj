(ns cdq.world-fns.modules.create-scaled-grid
  (:require [cdq.grid2d.utils :as helper]))

(defn do! [w]
  (assoc w :scaled-grid (helper/scale-grid (:grid w) (:scale w))))
