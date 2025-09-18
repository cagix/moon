(ns cdq.application.desktop
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.utils :as utils]
            [clojure.walk :as walk])
  (:gen-class))

(defn- edn-resource [path]
  (->> path
       io/resource
       slurp
       (edn/read-string {:readers {'edn/resource edn-resource}})
       (walk/postwalk utils/require-resolve-symbols)))

(defn -main [path]
  (utils/pipeline {} (edn-resource path)))
