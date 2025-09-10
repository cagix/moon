(ns cdq.draw.filled-ellipse
  (:require [clojure.earlygrey.shape-drawer :as sd]
            [clojure.gdx.graphics.color :as color]))

(defn do!
  [{:keys [ctx/shape-drawer]}
   [x y] radius-x radius-y color]
  (sd/set-color! shape-drawer (color/->obj color))
  (sd/filled-ellipse! shape-drawer x y radius-x radius-y))
