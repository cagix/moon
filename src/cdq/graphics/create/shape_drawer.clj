(ns cdq.graphics.create.shape-drawer
  (:require [clojure.gdx.graphics.g2d.texture-region :as texture-region])
  (:import (space.earlygrey.shapedrawer ShapeDrawer)))

(defn create
  [{:keys [graphics/batch
           graphics/shape-drawer-texture]
    :as graphics}]
  (assoc graphics :graphics/shape-drawer (ShapeDrawer. batch (texture-region/create shape-drawer-texture 1 0 1 1))))
