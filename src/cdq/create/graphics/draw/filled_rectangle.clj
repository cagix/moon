(ns cdq.create.graphics.draw.filled-rectangle
  (:require [clojure.graphics.shape-drawer :as sd]))

(defn do!
  [{:keys [graphics/shape-drawer]}
   x y w h color]
  (sd/set-color! shape-drawer color)
  (sd/filled-rectangle! shape-drawer x y w h))
