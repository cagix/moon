(ns cdq.graphics.shape-drawer
  (:require [com.badlogic.gdx.graphics.texture :as texture]
            [space.earlygrey.shape-drawer :as sd]))

(defn create
  [{:keys [graphics/batch
           graphics/shape-drawer-texture]
    :as graphics}]
  (assoc graphics :graphics/shape-drawer (sd/create batch (texture/region shape-drawer-texture 1 0 1 1))))
