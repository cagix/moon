(ns clojure.gdx.backends.lwjgl
  (:require [com.badlogic.gdx.application.listener :as listener]
            [com.badlogic.gdx.backends.lwjgl3.application :as application]
            [com.badlogic.gdx.backends.lwjgl3.application.config :as config]))

(defn create [listener config]
  (application/create (listener/create listener)
                      (config/create config)))
