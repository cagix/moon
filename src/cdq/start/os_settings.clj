(ns cdq.start.os-settings
  (:require [clojure.gdx.utils.shared-library-loader :as shared-library-loader]))

(defn do!
  [ctx os->executions]
  (doseq [[f params] (os->executions (shared-library-loader/operating-system))]
    (f params))
  ctx)
