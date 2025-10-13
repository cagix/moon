(ns cdq.application
  (:require [clojure.config :as config]
            [clojure.gdx :as gdx]
            [clojure.gdx.application.listener :as listener]
            [clojure.gdx.backends.lwjgl.application :as application]
            [clojure.gdx.backends.lwjgl.application.config :as app-config]
            [clojure.lwjgl.system.configuration :as lwjgl-config])
  (:gen-class))

(def state (atom nil))

(defn -main []
  (let [{:keys [listener config]} (config/edn-resource "runs.edn")]
    (lwjgl-config/set-glfw-library-name! "glfw_async")
    (application/create (listener/create
                         (let [{:keys [create
                                       dispose
                                       render
                                       resize]} listener]
                           {:create (fn []
                                      (reset! state (let [[f params] create]
                                                      ((requiring-resolve f) (gdx/context) params))))
                            :dispose (fn []
                                       ((requiring-resolve dispose) @state))
                            :render (fn []
                                      (swap! state (fn [ctx]
                                                     (let [[f params] render]
                                                       ((requiring-resolve f) ctx params)))))
                            :resize (fn [width height]
                                      ((requiring-resolve resize) @state width height))
                            :pause (fn [])
                            :resume (fn [])}))
                        (app-config/create config))))
