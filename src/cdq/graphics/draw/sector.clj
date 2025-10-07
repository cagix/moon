(ns cdq.graphics.draw.sector
  (:require [clojure.math :as math]
            [clojure.gdx.graphics.color :as color])
  (:import (space.earlygrey.shapedrawer ShapeDrawer)))

(defn do!
  [{:keys [^ShapeDrawer graphics/shape-drawer]}
   [center-x center-y] radius start-angle degree color]
  (.setColor shape-drawer (color/float-bits color))
  (.sector shape-drawer
           center-x
           center-y
           radius
           (math/to-radians start-angle)
           (math/to-radians degree)))
