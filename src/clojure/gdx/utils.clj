(ns clojure.gdx.utils
  (:require [com.badlogic.gdx.utils.shared-library-loader :as shared-library-loader]))

(defn dispatch-on-os
  [os->executions]
  (doseq [[f params] (os->executions (shared-library-loader/operating-system))]
    (f params)))
