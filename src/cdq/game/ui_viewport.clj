(ns cdq.game.ui-viewport
  (:require [cdq.ctx :as ctx]
            [cdq.utils :as utils]
            [gdl.graphics :as graphics]))

(defn do! []
  (utils/bind-root #'ctx/ui-viewport (graphics/ui-viewport ctx/ui-viewport-config)))
