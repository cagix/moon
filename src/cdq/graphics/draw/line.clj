(ns cdq.graphics.draw.line
  (:require [com.badlogic.gdx.graphics.color :as color]
            [space.earlygrey.shape-drawer :as sd]))

(defn do!
  [{:keys [graphics/shape-drawer]}
   [sx sy] [ex ey] color]
  (sd/set-color! shape-drawer (color/float-bits color))
  (sd/line! shape-drawer sx sy ex ey))
