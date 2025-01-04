(ns cdq.start
  "Application entry point."
  (:require [gdl.app :as app]
            [gdl.utils :refer [read-edn-resource]]
            [cdq.game :as game]))

(defn -main
  "Reads the application config from `app.edn` in resources and starts the game."
  []
  (let [config (read-edn-resource "app.edn")]
    (app/start (:app     config)
               (:context config)
               game/process-frame)))
