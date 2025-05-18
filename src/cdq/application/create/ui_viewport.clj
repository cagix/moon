(ns cdq.application.create.ui-viewport
  (:require [cdq.ctx :as ctx]
            [cdq.utils :refer [bind-root]]
            [gdl.graphics :as graphics]))

(defn do! []
  (bind-root #'ctx/ui-viewport (graphics/ui-viewport (:ui-viewport ctx/config))))
