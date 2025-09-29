(ns com.badlogic.gdx.backends.lwjgl3.application
  (:require [com.badlogic.gdx.backends.lwjgl3.application.configuration :as config]
            [org.lwjgl.system.configuration :as lwjgl-system])
  (:import (com.badlogic.gdx.backends.lwjgl3 Lwjgl3Application)))

(defn start! [listener config]
  (Lwjgl3Application. listener
                      (config/create config)))

(defn set-glfw-async! []
  (lwjgl-system/set-glfw-library-name! "glfw_async"))
