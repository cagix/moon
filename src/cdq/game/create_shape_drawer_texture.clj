(ns cdq.game.create-shape-drawer-texture
  (:require [cdq.ctx :as ctx]
            [cdq.utils :as utils]
            [gdl.graphics :as graphics]))

(defn do! []
  (utils/bind-root #'ctx/shape-drawer-texture (graphics/white-pixel-texture)))
