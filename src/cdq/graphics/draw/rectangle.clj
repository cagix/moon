(ns cdq.graphics.draw.rectangle
  (:require [clojure.gdx.graphics.color :as color])
  (:import (space.earlygrey.shapedrawer ShapeDrawer)))

(defn do!
  [{:keys [^ShapeDrawer graphics/shape-drawer]}
   x y w h color]
  (.setColor shape-drawer (color/float-bits color))
  (.rectangle shape-drawer x y w h))
