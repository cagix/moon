(ns cdq.draw.filled-ellipse
  (:require [cdq.gdx.graphics.color :as color]
            [cdq.gdx.graphics.shape-drawer :as sd]))

(defn draw!
  [[_ [x y] radius-x radius-y color]
   {:keys [ctx/shape-drawer]}]
  (sd/set-color! shape-drawer (color/->obj color))
  (sd/filled-ellipse! shape-drawer x y radius-x radius-y))
