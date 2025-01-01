(ns cdq.start
  (:require [cdq.game-loop :as game-loop]
            [clojure.utils :refer [read-edn-resource]]
            [gdl.app :as app]))

(defn -main []
  (let [config (read-edn-resource "app.edn")]
    (app/start (:app     config)
               (:context config)
               game-loop/render)))
