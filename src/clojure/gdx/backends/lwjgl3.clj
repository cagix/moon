(ns clojure.gdx.backends.lwjgl3
  (:import (com.badlogic.gdx.backends.lwjgl3 Lwjgl3Application
                                             Lwjgl3ApplicationConfiguration)
           (org.lwjgl.system Configuration)))

(defn application [app-listener config]
  (Lwjgl3Application. app-listener config))

(defn config [{:keys [title fps width height]}]
  (doto (Lwjgl3ApplicationConfiguration.)
    (.setTitle title)
    (.setForegroundFPS fps)
    (.setWindowedMode width height)))

(defn configure-glfw-for-mac []
  (.set Configuration/GLFW_LIBRARY_NAME "glfw_async")
  (.set Configuration/GLFW_CHECK_THREAD0 false))
