(ns cdq.draw.filled-ellipse
  (:require [cdq.graphics.shape-drawer :as sd]
            [clojure.gdx.graphics.color :as color]))

(defn draw!
  [[_ [x y] radius-x radius-y color]
   {:keys [ctx/shape-drawer]}]
  (sd/set-color! shape-drawer (color/->obj color))
  (sd/filled-ellipse! shape-drawer x y radius-x radius-y))
