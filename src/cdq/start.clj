(ns cdq.start
  (:require [cdq.application :as application]
            [cdq.config :as config]))

(defn -main []
  (-> "config.edn"
      config/create
      application/start!))
