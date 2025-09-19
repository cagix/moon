(ns com.badlogic.gdx.backends.lwjgl3
  (:require [clojure.config :as config]
            [com.badlogic.gdx.utils.shared-library-loader :as shared-library-loader])
  (:gen-class))

; 1. add 'operating-system' to ctx somehow! can re-use

(defn start-application! [listener config]
  (reduce (fn [ctx f]
            (f ctx))
          {:init/listener listener
           :init/config config}
          (config/edn-resource "com.badlogic.gdx.backends.lwjgl3.init.edn")))

(defn- call [[f params]]
  (f params))

(defn start! [{:keys [os->executions listener config]}]
  (doseq [[f params] (os->executions (shared-library-loader/operating-system))]
    (f params))
  (start-application! (call listener)
                      config))

(defn -main [path]
  (-> path
      config/edn-resource
      start!))
