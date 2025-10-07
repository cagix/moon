(ns cdq.graphics.draw.circle
  (:require [clojure.gdx.graphics.color :as color])
  (:import (space.earlygrey.shapedrawer ShapeDrawer)))

(defn do!
  [{:keys [^ShapeDrawer graphics/shape-drawer]}
   [x y] radius color]
  (.setColor shape-drawer (color/float-bits color))
  (.circle shape-drawer x y radius))
