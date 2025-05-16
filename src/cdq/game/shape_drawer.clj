(ns cdq.game.shape-drawer
  (:require [cdq.ctx :as ctx]
            [cdq.utils :as utils]
            [clojure.graphics :as graphics]))

(defn do! []
  (utils/bind-root #'ctx/shape-drawer
                   (graphics/shape-drawer ctx/batch
                                          (graphics/texture-region ctx/shape-drawer-texture 1 0 1 1))))
