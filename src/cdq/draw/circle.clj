(ns cdq.draw.circle
  (:require [cdq.graphics.color :as color]
            [cdq.graphics.shape-drawer :as sd]))

(defn draw!
  [[_ [x y] radius color]
   {:keys [ctx/shape-drawer]}]
  (sd/set-color! shape-drawer (color/->obj color))
  (sd/circle! shape-drawer x y radius))
