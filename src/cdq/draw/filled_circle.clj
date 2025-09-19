(ns cdq.draw.filled-circle
  (:require [space.earlygrey.shape-drawer :as sd]))

(defn do!
  [{:keys [ctx/shape-drawer]}
   [x y] radius color]
  (sd/set-color! shape-drawer color)
  (sd/filled-circle! shape-drawer x y radius))
