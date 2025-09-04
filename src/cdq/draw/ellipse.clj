(ns cdq.draw.ellipse
  (:require [cdq.graphics.color :as color]
            [cdq.graphics.shape-drawer :as sd]))

(defn draw!
  [[_ [x y] radius-x radius-y color]
   {:keys [ctx/shape-drawer]}]
  (sd/set-color! shape-drawer (color/->obj color))
  (sd/ellipse! shape-drawer x y radius-x radius-y))
