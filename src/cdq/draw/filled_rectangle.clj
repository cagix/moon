(ns cdq.draw.filled-rectangle
  (:require [cdq.graphics.color :as color]
            [cdq.graphics.shape-drawer :as sd]))

(defn draw!
  [[_ x y w h color]
   {:keys [ctx/shape-drawer]}]
  (sd/set-color! shape-drawer (color/->obj color))
  (sd/filled-rectangle! shape-drawer x y w h))
