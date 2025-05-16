(ns cdq.game.create-sprite-batch
  (:require [cdq.ctx :as ctx]
            [cdq.utils :as utils]
            [gdl.graphics :as graphics]))

(defn do! []
  (utils/bind-root #'ctx/batch (graphics/sprite-batch)))
