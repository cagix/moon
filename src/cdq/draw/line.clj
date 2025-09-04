(ns cdq.draw.line
  (:require [cdq.graphics.color :as color]
            [cdq.graphics.shape-drawer :as sd]))

(defn draw!
  [[_ [sx sy] [ex ey] color]
   {:keys [ctx/shape-drawer]}]
  (sd/set-color! shape-drawer (color/->obj color))
  (sd/line! shape-drawer sx sy ex ey))
