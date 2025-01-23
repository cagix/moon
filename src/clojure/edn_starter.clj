(ns clojure.edn-starter
  (:require [cdq.utils :as utils]
            [clojure.edn :as edn]
            [clojure.java.io :as io]))

(defn -main [edn-file]
  (-> edn-file
      io/resource
      slurp
      edn/read-string
      utils/execute!))
