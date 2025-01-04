(ns cdq.start
  "Application entry point."
  (:require [gdl.app :as app]))

(defn -main
  "Reads the application config from `app.edn` in resources and starts the game."
  []
  (app/start "app.edn"))
