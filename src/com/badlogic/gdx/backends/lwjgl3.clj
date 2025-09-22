(ns com.badlogic.gdx.backends.lwjgl3
  (:require [com.badlogic.gdx.application-listener :as application-listener]
            [com.badlogic.gdx.backends.lwjgl3.application :as application]
            [com.badlogic.gdx.backends.lwjgl3.application.config :as application-config]))

(defn start-application!
  [listener config]
  (application/start! (application-listener/create listener)
                      (application-config/create config)))
