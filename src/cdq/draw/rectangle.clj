(ns cdq.draw.rectangle
  (:require [com.badlogic.gdx.graphics.color :as color]
            [gdl.graphics.shape-drawer :as sd]))

(defn do!
  [{:keys [graphics/shape-drawer]}
   x y w h color]
  (sd/set-color! shape-drawer (color/create color))
  (sd/rectangle! shape-drawer x y w h))
