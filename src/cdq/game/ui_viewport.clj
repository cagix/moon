(ns cdq.game.ui-viewport
  (:require [cdq.ctx :as ctx]
            [cdq.impl.graphics :as graphics]
            [cdq.utils :as utils]))

(defn do! []
  (utils/bind-root #'ctx/ui-viewport (graphics/fit-viewport 1440 900)))
