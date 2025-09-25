(ns com.badlogic.gdx.backends.lwjgl3
  (:require [com.badlogic.gdx.backends.lwjgl3.application.config :as application-config])
  (:import (com.badlogic.gdx.backends.lwjgl3 Lwjgl3Application)))

(defn application [listener config]
  (Lwjgl3Application. listener
                      (application-config/create config)))
