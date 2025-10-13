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
                         (let [{:keys [state
                                       create
                                       dispose
                                       render
                                       resize]} listener]
                           (let [state @(requiring-resolve state)]
                             {:create (fn []
                                        (reset! state (let [[f params] create]
                                                        ((requiring-resolve f) params))))
                              :dispose (fn []
                                         ((requiring-resolve dispose) @state))
                              :render (fn []
                                        (swap! state (fn [ctx]
                                                       (let [[f params] render]
                                                         ((requiring-resolve f) ctx params)))))
                              :resize (fn [width height]
                                        ((requiring-resolve resize) @state width height))
                              :pause (fn [])
                              :resume (fn [])})))
                        (app-config/create config))))
