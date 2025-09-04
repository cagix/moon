(ns cdq.draw.rectangle
  (:require [cdq.gdx.graphics.color :as color]
            [cdq.gdx.graphics.shape-drawer :as sd]))

(defn draw!
  [[_ x y w h color]
   {:keys [g/shape-drawer]}]
  (sd/set-color! shape-drawer (color/->obj color))
  (sd/rectangle! shape-drawer x y w h))
