(ns cdq.draw.line
  (:require [cdq.graphics.shape-drawer :as sd]
            [clojure.gdx.graphics.color :as color]))

(defn draw!
  [[_ [sx sy] [ex ey] color]
   {:keys [ctx/shape-drawer]}]
  (sd/set-color! shape-drawer (color/->obj color))
  (sd/line! shape-drawer sx sy ex ey))
