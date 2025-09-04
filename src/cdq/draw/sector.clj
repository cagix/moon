(ns cdq.draw.sector
  (:require [cdq.graphics.color :as color]
            [cdq.graphics.shape-drawer :as sd]
            [cdq.math :as math]))

(defn draw!
  [[_ [center-x center-y] radius start-angle degree color]
   {:keys [ctx/shape-drawer]}]
  (sd/set-color! shape-drawer (color/->obj color))
  (sd/sector! shape-drawer
              center-x
              center-y
              radius
              (math/degree->radians start-angle)
              (math/degree->radians degree)))
