(ns cdq.draw.circle
  (:require [cdq.gdx.graphics.color :as color]
            [cdq.gdx.graphics.shape-drawer :as sd]))

(defn draw!
  [[_ [x y] radius color]
   {:keys [g/shape-drawer]}]
  (sd/set-color! shape-drawer (color/->obj color))
  (sd/circle! shape-drawer x y radius))
