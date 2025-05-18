(ns cdq.application.create.schemas
  (:require [cdq.ctx :as ctx]
            [cdq.utils :refer [bind-root
                               io-slurp-edn]]))

(defn do! []
  (bind-root #'ctx/schemas (io-slurp-edn (:schemas ctx/config))))
