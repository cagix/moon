(ns cdq.application.edn
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [cdq.utils :as utils]))

(defn -main [app-edn]
  (-> app-edn
      io/resource
      slurp
      edn/read-string
      utils/execute!))
