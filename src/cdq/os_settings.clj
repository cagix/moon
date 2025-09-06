(ns cdq.os-settings
  (:require [clojure.gdx.utils.shared-library-loader :as shared-library-loader]))

(defn dispatch! [os->executions]
  (println "os: " (shared-library-loader/operating-system))
  (doseq [[f params] (get os->executions (shared-library-loader/operating-system))]
    (println "dispatch " f)
    ((requiring-resolve f) params)))
