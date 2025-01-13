(ns clojure.application
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]))

(defn -main []
  (doseq [[qualified-symbol params] (-> "clojure.application.edn"
                                        io/resource
                                        slurp
                                        edn/read-string)
          :let [ns (symbol (namespace qualified-symbol))
                f (resolve qualified-symbol)]]
    (require ns)
    (if params (f params) (f))))
