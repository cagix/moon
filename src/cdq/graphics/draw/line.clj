(ns cdq.graphics.draw.line
  (:require [clojure.gdx.graphics.color :as color])
  (:import (space.earlygrey.shapedrawer ShapeDrawer)))

(defn do!
  [{:keys [^ShapeDrawer graphics/shape-drawer]}
   [sx sy] [ex ey] color]
  (.setColor shape-drawer (color/float-bits color))
  (.line shape-drawer (float sx) (float sy) (float ex) (float ey)))
