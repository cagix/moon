(ns cdq.draw.ellipse
  (:require [clojure.earlygrey.shape-drawer :as sd]
            [clojure.gdx.graphics.color :as color]))

(defn do!
  [[_ [x y] radius-x radius-y color]
   {:keys [ctx/shape-drawer]}]
  (sd/set-color! shape-drawer (color/->obj color))
  (sd/ellipse! shape-drawer x y radius-x radius-y))
