(ns clojure.gdx.backends.lwjgl3
  (:require [clojure.java.io :as io])
  (:import (com.badlogic.gdx ApplicationAdapter)
           (com.badlogic.gdx.backends.lwjgl3 Lwjgl3Application
                                             Lwjgl3ApplicationConfiguration)
           (com.badlogic.gdx.utils SharedLibraryLoader)
           (java.awt Taskbar Toolkit)
           (org.lwjgl.system Configuration)))

(defn application [{:keys [icon
                           title
                           window-width
                           window-height
                           foreground-fps
                           create
                           dispose
                           render
                           resize]}]
  (.setIconImage (Taskbar/getTaskbar)
                 (.getImage (Toolkit/getDefaultToolkit)
                            (io/resource icon)))
  (when SharedLibraryLoader/isMac
    (.set Configuration/GLFW_LIBRARY_NAME "glfw_async"))
  (Lwjgl3Application. (proxy [ApplicationAdapter] []
                        (create []
                          (create))

                        (dispose []
                          (dispose))

                        (render []
                          (render))

                        (resize [width height]
                          (resize width height)))
                      (doto (Lwjgl3ApplicationConfiguration.)
                        (.setTitle title)
                        (.setWindowedMode window-width
                                          window-height)
                        (.setForegroundFPS foreground-fps))))
