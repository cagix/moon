(ns clojure.application
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.utils :as utils]))

(defn -main []
  (-> "clojure.application.edn"
      io/resource
      slurp
      edn/read-string
      utils/execute!))
