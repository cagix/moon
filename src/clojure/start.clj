(ns clojure.start
  (:require [clojure.config :as config]
            [clojure.object :as object])
  (:gen-class))

(defn -main []
  (-> "clojure.start.edn"
      config/edn-resource
      object/pipeline))
