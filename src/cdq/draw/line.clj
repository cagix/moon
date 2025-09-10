(ns cdq.draw.line
  (:require [clojure.earlygrey.shape-drawer :as sd]
            [clojure.gdx.graphics.color :as color]))

(defn do!
  [{:keys [ctx/shape-drawer]}
   [sx sy] [ex ey] color]
  (sd/set-color! shape-drawer (color/->obj color))
  (sd/line! shape-drawer sx sy ex ey))
