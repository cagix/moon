(ns cdq.application.create.graphics.shape-drawer
  (:require [clojure.utils]
            [com.badlogic.gdx.graphics.texture :as texture]
            [space.earlygrey.shape-drawer :as sd]))

(defn do!
  [{:keys [graphics/batch
           graphics/shape-drawer-texture]
    :as graphics}]
  (assoc graphics :graphics/shape-drawer (sd/create batch (texture/region shape-drawer-texture 1 0 1 1))))

(clojure.utils/extend-by [{:atype space.earlygrey.shapedrawer.ShapeDrawer
                           :implementation-ns 'space.earlygrey.shape-drawer
                           :protocol gdl.graphics.shape-drawer/ShapeDrawer}])
