(ns cdq.create.graphics.draw.line
  (:require [gdl.graphics.shape-drawer :as sd]))

(defn do!
  [{:keys [graphics/shape-drawer]}
   [sx sy] [ex ey] color]
  (sd/set-color! shape-drawer color)
  (sd/line! shape-drawer sx sy ex ey))
