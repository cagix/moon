(ns cdq.application.create.batch
  (:require [cdq.ctx :as ctx]
            [cdq.utils :refer [bind-root]]
            [gdl.graphics :as graphics]))

(defn do! []
  (bind-root #'ctx/batch (graphics/sprite-batch)))
