(ns cdq.graphics.draw.ellipse
  (:require [clojure.gdx.graphics.color :as color])
  (:import (space.earlygrey.shapedrawer ShapeDrawer)))

(defn do!
  [{:keys [^ShapeDrawer graphics/shape-drawer]}
   [x y] radius-x radius-y color]
  (.setColor shape-drawer (color/float-bits color))
  (.ellipse shape-drawer x y radius-x radius-y))
