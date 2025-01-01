(ns cdq.start
  (:require [gdl.app :as app]
            [cdq.context :as context]
            [clojure.utils :refer [read-edn-resource]]))

(defn -main []
  (let [config (read-edn-resource "app.edn")]
    (app/start (:app     config)
               (:context config)
               context/render-game)))
