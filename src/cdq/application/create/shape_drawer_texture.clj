(ns cdq.application.create.shape-drawer-texture
  (:require [cdq.ctx :as ctx]
            [cdq.utils :refer [bind-root]]
            [gdl.graphics :as graphics]))

(defn do! []
  (bind-root #'ctx/shape-drawer-texture (graphics/white-pixel-texture)))


