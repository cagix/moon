(ns cdq.game.ui-viewport
  (:require [cdq.ctx :as ctx]
            [cdq.utils :as utils]
            [clojure.graphics :as graphics]))

(defn do! []
  (utils/bind-root #'ctx/ui-viewport (graphics/ui-viewport 1440 900)))
