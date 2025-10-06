(ns cdq.graphics.draw.filled-rectangle
  (:require [clojure.gdx.graphics.color :as color]
            [clojure.gdx.shape-drawer :as sd]))

(defn do!
  [{:keys [graphics/shape-drawer]}
   x y w h color]
  (sd/set-color! shape-drawer (color/float-bits color))
  (sd/filled-rectangle! shape-drawer x y w h))
