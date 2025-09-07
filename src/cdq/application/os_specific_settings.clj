(ns cdq.application.os-specific-settings
  (:require [clojure.gdx.utils.shared-library-loader :as shared-library-loader]))

(defn handle! [{:keys [ctx/os-settings]}]
  (->> (shared-library-loader/operating-system)
       os-settings
       (run! (fn [[f params]]
               ((requiring-resolve f) params)))))
