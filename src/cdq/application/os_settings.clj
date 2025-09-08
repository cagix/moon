(ns cdq.application.os-settings
  (:require [clojure.gdx.utils.shared-library-loader :as shared-library-loader]))

(defn handle!
  [{:keys [ctx/os-settings]
    :as ctx}]
  (->> (shared-library-loader/operating-system)
       os-settings
       (run! (fn [[f params]]
               (println [f params])
               ((requiring-resolve f) params))))
  ctx)
