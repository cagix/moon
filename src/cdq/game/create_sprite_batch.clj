(ns cdq.game.create-sprite-batch
  (:require [cdq.ctx :as ctx]
            [cdq.graphics :as graphics]
            [cdq.utils :as utils]))

(defn do! []
  (utils/bind-root #'ctx/batch (graphics/sprite-batch)))
