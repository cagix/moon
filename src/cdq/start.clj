(ns cdq.start
  (:require [clojure.config :as config]
            [clojure.gdx.application.listener :as listener]
            [clojure.gdx.backends.lwjgl.application :as application]
            [clojure.gdx.backends.lwjgl.application.config :as app-config]
            [clojure.lwjgl.system.configuration :as lwjgl-config])
  (:gen-class))

(defn -main []
  (let [{:keys [listener config]} (config/edn-resource "runs.edn")]
    (lwjgl-config/set-glfw-library-name! "glfw_async")
    (application/create (listener/create
                         (let [[f & params] listener]
                           (apply (requiring-resolve f) params)))
                        (app-config/create config))))
