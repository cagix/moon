(ns clojure.edn-starter
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.utils :as utils]))

(defn -main [edn-file]
  (-> edn-file
      io/resource
      slurp
      edn/read-string
      utils/execute!))
