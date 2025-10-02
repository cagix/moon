(ns cdq.world-fns.modules.create-scaled-grid
  (:require [gdl.grid2d.utils :as helper]))

(defn step [w]
  (assoc w :scaled-grid (helper/scale-grid (:grid w) (:scale w))))
