(ns cdq.graphics.draw.ellipse
  (:require [clojure.color :as color]
            [clojure.gdx.graphics.g2d.shape-drawer :as sd]))

(defn do!
  [{:keys [graphics/shape-drawer]}
   [x y] radius-x radius-y color]
  (sd/set-color! shape-drawer (color/float-bits color))
  (sd/ellipse! shape-drawer x y radius-x radius-y))
