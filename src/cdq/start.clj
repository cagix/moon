(ns cdq.start
  (:require [clojure.edn :as edn]
            [clojure.gdx.backends.lwjgl :as lwjgl]
            [clojure.gdx.utils.shared-library-loader :as shared-library-loader]
            [clojure.java.io :as io])
  (:gen-class))

(defn- execute! [[f params]]
  ((requiring-resolve f) params))

(defn -main []
  (let [{:keys [operating-sytem->executables
                listener
                config]} (-> "cdq.start.edn" io/resource slurp edn/read-string)]
    (->> (shared-library-loader/operating-system)
         operating-sytem->executables
         (run! execute!))
    (lwjgl/start-application! (let [[f params] listener]
                                ((requiring-resolve f) params))
                              config)))
