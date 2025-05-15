(ns cdq.game.shape-drawer
  (:require [cdq.ctx :as ctx]
            [cdq.utils :as utils])
  (:import (space.earlygrey.shapedrawer ShapeDrawer)))

(defn do! []
  (utils/bind-root #'ctx/shape-drawer (ShapeDrawer. (:java-object ctx/batch)
                                                    ctx/shape-drawer-texture)))
