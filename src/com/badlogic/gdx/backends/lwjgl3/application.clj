(ns com.badlogic.gdx.backends.lwjgl3.application
  (:import (com.badlogic.gdx.backends.lwjgl3 Lwjgl3Application)))

(defn start! [listener config]
  (Lwjgl3Application. listener config))
