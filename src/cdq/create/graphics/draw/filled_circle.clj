(ns cdq.create.graphics.draw.filled-circle
  (:require [gdl.graphics.shape-drawer :as sd]))

(defn do!
  [{:keys [graphics/shape-drawer]}
   [x y] radius color]
  (sd/set-color! shape-drawer color)
  (sd/filled-circle! shape-drawer x y radius))
