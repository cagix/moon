(ns cdq.graphics.create.shape-drawer
  (:require [com.badlogic.gdx.graphics.g2d.texture-region :as texture-region]
            [space.earlygrey.shape-drawer :as sd]))

(defn create
  [{:keys [graphics/batch
           graphics/shape-drawer-texture]
    :as graphics}]
  (assoc graphics :graphics/shape-drawer (sd/create batch (texture-region/create shape-drawer-texture 1 0 1 1))))
