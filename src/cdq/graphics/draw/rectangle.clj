(ns cdq.graphics.draw.rectangle
  (:require [clojure.color :as color]
            [clojure.gdx.graphics.g2d.shape-drawer :as sd]))

(defn do!
  [{:keys [graphics/shape-drawer]}
   x y w h color]
  (sd/set-color! shape-drawer (color/float-bits color))
  (sd/rectangle! shape-drawer x y w h))
