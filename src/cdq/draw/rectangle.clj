(ns cdq.draw.rectangle
  (:require [space.earlygrey.shape-drawer :as sd]))

(defn do!
  [{:keys [graphics/shape-drawer]}
   x y w h color]
  (sd/set-color! shape-drawer color)
  (sd/rectangle! shape-drawer x y w h))
