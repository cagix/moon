(ns com.badlogic.gdx.backends.lwjgl3
  (:require [clojure.config :as config])
  (:gen-class))

(defn start-application!
  [listener config os->executions]
  (reduce (fn [ctx f]
            (f ctx))
          {:init/listener listener
           :init/config config
           :init/os->executions (or os->executions {})}
          (config/edn-resource "com.badlogic.gdx.backends.lwjgl3.init.edn")))

(defn- call [[f params]]
  (f params))

(defn -main [path]
  (let [{:keys [os->executions
                listener
                config]} (-> path config/edn-resource)]
    (start-application! (call listener)
                        config
                        os->executions)))
