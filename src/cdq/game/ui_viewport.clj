(ns cdq.game.ui-viewport
  (:require [cdq.ctx :as ctx]
            [cdq.graphics :as graphics]
            [cdq.utils :as utils]))

(defn do! []
  (utils/bind-root #'ctx/ui-viewport (graphics/ui-viewport 1440 900)))
