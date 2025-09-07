(ns cdq.application.lwjgl
  (:require [clojure.gdx.backends.lwjgl :as lwjgl]
            [clojure.gdx.utils.shared-library-loader :as shared-library-loader]))

(defn execute! [[f params]]
  ((requiring-resolve f) params))

(defn start!
  [{:keys [operating-sytem->executables
           listener
           config]}]
  (->> (shared-library-loader/operating-system)
       operating-sytem->executables
       (run! execute!))
  (lwjgl/start-application! (let [[f params] listener]
                              ((requiring-resolve f) params))
                            config))
