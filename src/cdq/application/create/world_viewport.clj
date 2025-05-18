(ns cdq.application.create.world-viewport
  (:require [cdq.ctx :as ctx]
            [cdq.utils :refer [bind-root]]
            [gdl.graphics :as graphics]))

(defn do! []
  (bind-root #'ctx/world-viewport (graphics/world-viewport ctx/world-unit-scale
                                                           (:world-viewport ctx/config))))
