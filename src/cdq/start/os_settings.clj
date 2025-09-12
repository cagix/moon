(ns cdq.start.os-settings
  (:require [clojure.gdx.utils.shared-library-loader :as shared-library-loader]))

(defn do!
  [{:keys [ctx/config]
    :as ctx}]
  (doseq [[f params] ((:cdq.start.os-settings config)
                      (shared-library-loader/operating-system))]
    (f params))
  ctx)
