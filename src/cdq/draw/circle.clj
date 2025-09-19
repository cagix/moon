(ns cdq.draw.circle
  (:require [space.earlygrey.shape-drawer :as sd]))

(defn do!
  [{:keys [graphics/shape-drawer]}
   [x y] radius color]
  (sd/set-color! shape-drawer color)
  (sd/circle! shape-drawer x y radius))
