(ns cdq.graphics.create.shape-drawer
  (:import (com.badlogic.gdx.graphics.g2d TextureRegion)
           (space.earlygrey.shapedrawer ShapeDrawer)))

(defn create
  [{:keys [graphics/batch
           graphics/shape-drawer-texture]
    :as graphics}]
  (assoc graphics
         :graphics/shape-drawer
         (ShapeDrawer. batch
                       (TextureRegion. shape-drawer-texture
                                       1
                                       0
                                       1
                                       1))))
