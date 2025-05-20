(ns cdq.application.create.config
  (:require [cdq.ctx :as ctx]
            [cdq.utils :refer [bind-root
                               create-config]]))

(defn do! []
  (bind-root #'ctx/config (create-config "config.edn")))
