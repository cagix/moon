(ns com.badlogic.gdx.backends.lwjgl3
  (:require [clojure.utils :as utils]
            [com.badlogic.gdx.application-listener :as application-listener]
            [com.badlogic.gdx.backends.lwjgl3.application :as application]
            [com.badlogic.gdx.backends.lwjgl3.application.config :as application-config]))

(defn start-application!
  [{:keys [listener config]}]
  (application/start! (application-listener/create (utils/execute listener))
                      (application-config/create config)))
