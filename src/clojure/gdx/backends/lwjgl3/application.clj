(ns clojure.gdx.backends.lwjgl3.application
  (:require [clojure.java.io :as io])
  (:import (java.awt Taskbar Toolkit)
           (com.badlogic.gdx.backends.lwjgl3 Lwjgl3Application
                                             Lwjgl3ApplicationConfiguration)
           (com.badlogic.gdx.utils SharedLibraryLoader)
           (org.lwjgl.system Configuration)))

(defn create [application-listener config]
  (.setIconImage (Taskbar/getTaskbar)
                 (.getImage (Toolkit/getDefaultToolkit)
                            (io/resource (:icon config))))
  (when SharedLibraryLoader/isMac
    (.set Configuration/GLFW_LIBRARY_NAME "glfw_async")
    (.set Configuration/GLFW_CHECK_THREAD0 false))
  (Lwjgl3Application. application-listener
                      (doto (Lwjgl3ApplicationConfiguration.)
                        (.setTitle (:title config))
                        (.setWindowedMode (:window-width config)
                                          (:window-height config))
                        (.setForegroundFPS (:fps config)))))
