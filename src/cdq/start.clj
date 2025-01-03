(ns cdq.start
  "Application entry point."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [gdl.app :as app]
            [cdq.game :as game]))

(defn -main
  "Reads the application config from `app.edn` in resources and starts the game."
  []
  (let [config (-> "app.edn" io/resource slurp edn/read-string)]
    (app/start (:app     config)
               (:context config)
               game/process-frame)))
