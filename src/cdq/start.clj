(ns cdq.start
  (:require [cdq.application :as application]
            [cdq.config :as config]))

(defn -main [config-path]
  (-> config-path
      config/create
      application/start!))
