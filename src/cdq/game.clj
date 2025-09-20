(ns cdq.game
  (:require [clojure.config :as config]
            [com.badlogic.gdx.backends.lwjgl3 :as lwjgl])
  (:gen-class))

(defn- call [[f params]]
  (f params))

(defn -main [path]
  (let [{:keys [os->executions
                listener
                config]} (-> path config/edn-resource)]
    (lwjgl/start-application! (call listener)
                              config
                              os->executions)))
