(ns cdq.application
  (:require [cdq.game :as game]
            [clojure.config :as config]
            [clojure.gdx :as gdx]
            [clojure.gdx.application.listener :as listener]
            [clojure.gdx.backends.lwjgl.application :as application]
            [clojure.gdx.backends.lwjgl.application.config :as app-config]
            [clojure.lwjgl.system.configuration :as lwjgl-config])
  (:gen-class))

(def state (atom nil))

(defn -main []
  (let [config (config/edn-resource "config.edn")]
    (lwjgl-config/set-glfw-library-name! "glfw_async")
    (application/create (listener/create
                         {:create (fn []
                                    (reset! state (game/create! (gdx/context) config)))

                          :dispose (fn []
                                     (game/dispose! @state))

                          :render (fn []
                                    (swap! state game/render!))

                          :resize (fn [width height]
                                    (game/resize! @state width height))

                          :pause (fn [])
                          :resume (fn [])})
                        (app-config/create (:lwjgl-application config)))))
