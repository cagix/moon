(ns cdq.application.create.shape-drawer
  (:require [cdq.ctx :as ctx]
            [cdq.utils :refer [bind-root]]
            [gdl.graphics :as graphics]))

(defn do! []
  (bind-root #'ctx/shape-drawer (graphics/shape-drawer ctx/batch
                                                       (graphics/texture-region ctx/shape-drawer-texture 1 0 1 1))))
