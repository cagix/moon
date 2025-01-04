(ns cdq.start
  "Application entry point."
  (:require [gdl.app :as app]
            [gdl.utils :refer [read-edn-resource]]
            [cdq.game :as game]))

(defn -main
  "Reads the application config from `app.edn` in resources and starts the game."
  []
  (app/start (read-edn-resource "app.edn")
             game/process-frame))
