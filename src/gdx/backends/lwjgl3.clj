(ns gdx.backends.lwjgl3
  (:import (com.badlogic.gdx.backends.lwjgl3 Lwjgl3Application Lwjgl3ApplicationConfiguration)
           (com.badlogic.gdx.utils SharedLibraryLoader)
           (java.awt Taskbar Toolkit)
           (org.lwjgl.system Configuration)))

(defn- set-dock-icon [image-path]
  (let [toolkit (Toolkit/getDefaultToolkit)
        image (.getImage toolkit (clojure.java.io/resource image-path))
        taskbar (Taskbar/getTaskbar)]
    (.setIconImage taskbar image)))

(defn application [application-listener {:keys [title fps width height dock-icon]}]
  (when SharedLibraryLoader/isMac
    (set-dock-icon dock-icon)
    (.set Configuration/GLFW_LIBRARY_NAME "glfw_async")
    (.set Configuration/GLFW_CHECK_THREAD0 false))
  (Lwjgl3Application. application-listener
                      (doto (Lwjgl3ApplicationConfiguration.)
                        (.setTitle title)
                        (.setForegroundFPS fps)
                        (.setWindowedMode width height))))
