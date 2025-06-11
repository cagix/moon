(ns gdl.application.desktop
  (:require [clojure.java.io :as io])
  (:import (com.badlogic.gdx ApplicationListener)
           (com.badlogic.gdx.backends.lwjgl3 Lwjgl3Application
                                             Lwjgl3ApplicationConfiguration)
           (com.badlogic.gdx.utils SharedLibraryLoader
                                   Os)
           (java.awt Taskbar
                     Toolkit)
           (org.lwjgl.system Configuration)))

(defn- apply-mac-os-settings!
  [{:keys [glfw-async?
           dock-icon]}]
  (when glfw-async?
    (.set Configuration/GLFW_LIBRARY_NAME "glfw_async"))
  (when dock-icon
    (.setIconImage (Taskbar/getTaskbar)
                   (.getImage (Toolkit/getDefaultToolkit)
                              (io/resource dock-icon)))))

(defn start! [{:keys [mac-os-settings
                      create
                      dispose
                      render
                      resize
                      title
                      windowed-mode
                      foreground-fps]}]
  (when (= SharedLibraryLoader/os Os/MacOsX)
    (apply-mac-os-settings! mac-os-settings))
  (Lwjgl3Application. (proxy [ApplicationListener] []
                        (create []
                          (let [[f params] create]
                            (f params)))
                        (dispose []
                          (dispose))
                        (render  []
                          (let [[f params] render]
                            (f params)))
                        (resize [width height]
                          (resize width height))
                        (pause [])
                        (resume []))
                      (doto (Lwjgl3ApplicationConfiguration.)
                        (.setTitle title)
                        (.setWindowedMode (:width  windowed-mode)
                                          (:height windowed-mode))
                        (.setForegroundFPS foreground-fps))))
