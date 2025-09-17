(ns cdq.start
  (:require [clojure.edn :as edn]
            [clojure.gdx.utils]
            [clojure.java.io :as io]
            [clojure.utils :as utils]
            [clojure.walk :as walk]
            [com.badlogic.gdx.backends.lwjgl3 :as lwjgl3])
  (:gen-class))

(defn- call [[f params]]
  (f params))

(defn- start!
  [{:keys [os-settings
           config
           listener]}]
  (clojure.gdx.utils/dispatch-on-os os-settings)
  (lwjgl3/start-application! (call listener)
                             config))

(defn- edn-resource [path]
  (->> path
       io/resource
       slurp
       (edn/read-string {:readers {'edn/resource edn-resource}})
       (walk/postwalk utils/require-resolve-symbols)))

(defn -main [path]
  (start! (edn-resource path)))
