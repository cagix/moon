(ns clojure.gdx.backends.lwjgl
  (:require [clojure.java.io :as io])
  (:import (com.badlogic.gdx.backends.lwjgl3 Lwjgl3Application Lwjgl3ApplicationConfiguration)
           (com.badlogic.gdx.utils SharedLibraryLoader Os)
           (java.awt Taskbar Toolkit)
           (org.lwjgl.system Configuration)))

(defn application! [{:keys [title
                            windowed-mode
                            foreground-fps
                            dock-icon]}
                    application-listener]
  (when (= SharedLibraryLoader/os Os/MacOsX)
    (.setIconImage (Taskbar/getTaskbar)
                   (.getImage (Toolkit/getDefaultToolkit)
                              (io/resource dock-icon)))
    (.set Configuration/GLFW_LIBRARY_NAME "glfw_async"))
  (Lwjgl3Application. application-listener
                      (doto (Lwjgl3ApplicationConfiguration.)
                        (.setTitle title)
                        (.setWindowedMode (:width  windowed-mode)
                                          (:height windowed-mode))
                        (.setForegroundFPS foreground-fps))))
