(ns gdl.os-settings
  (:require [com.badlogic.gdx.utils.shared-library-loader :as shared-library-loader]
            [com.badlogic.gdx.utils.os :as os]))

(defn do! [os->executions]
  (doseq [[f params] (get os->executions (os/value->keyword shared-library-loader/os))]
    ((requiring-resolve f) params)))
