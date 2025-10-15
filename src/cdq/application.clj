(ns cdq.application
  (:require [cdq.game :as game]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.gdx :as gdx]
            [clojure.gdx.application.listener :as listener]
            [clojure.gdx.backends.lwjgl.application :as application]
            [clojure.gdx.backends.lwjgl.application.config :as app-config]
            [clojure.lwjgl.system.configuration :as lwjgl-config])
  (:gen-class))

(def state (atom nil))

(defn create-listener [config]
  (listener/create
   {:create (fn []
              (reset! state (game/create! (gdx/context) config)))

    :dispose (fn []
               (game/dispose! @state))

    :render (fn []
              (swap! state game/render!))

    :resize (fn [width height]
              (game/resize! @state width height))

    :pause (fn [])
    :resume (fn [])}))

(defn call [[f & params]]
  (apply f params))

(defn create-app-config [opts]
  (app-config/create opts))

(defn start-lwjgl-app!
  [{:keys [listener
           config]}]
  (application/create (call listener)
                      (call config)))


(defn edn-resource [path]
  (->> path
       io/resource
       slurp
       (edn/read-string {:readers {'edn/resource edn-resource}})))

(defn -main []
  (let [config (edn-resource "config.edn")]
    (doseq [command [
                     [clojure.lwjgl.system.configuration/set-glfw-library-name! "glfw_async"]
                     [start-lwjgl-app! {:listener [create-listener config]
                                        :config [create-app-config (:lwjgl-application config)]}]
                     ]]
      (call command))))
