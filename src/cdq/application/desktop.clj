(ns cdq.application.desktop
  (:require [clojure.java.io :as io])
  (:import (com.badlogic.gdx.backends.lwjgl3 Lwjgl3Application Lwjgl3ApplicationConfiguration)
           (com.badlogic.gdx.utils SharedLibraryLoader Os)
           (java.awt Taskbar Toolkit)
           (org.lwjgl.system Configuration)))

(defn application! [config listener]
  (when (= SharedLibraryLoader/os Os/MacOsX)
    (.setIconImage (Taskbar/getTaskbar)
                   (.getImage (Toolkit/getDefaultToolkit)
                              (io/resource (:dock-icon (:mac-os config)))))
    (.set Configuration/GLFW_LIBRARY_NAME "glfw_async"))
  (Lwjgl3Application. listener
                      (doto (Lwjgl3ApplicationConfiguration.)
                        (.setTitle (:title config))
                        (.setWindowedMode (:width  (:windowed-mode config))
                                          (:height (:windowed-mode config)))
                        (.setForegroundFPS (:foreground-fps config)))))
