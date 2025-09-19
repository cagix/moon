(ns cdq.draw.ellipse
  (:require [space.earlygrey.shape-drawer :as sd]))

(defn do!
  [{:keys [ctx/shape-drawer]}
   [x y] radius-x radius-y color]
  (sd/set-color! shape-drawer color)
  (sd/ellipse! shape-drawer x y radius-x radius-y))
