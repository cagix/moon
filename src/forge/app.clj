(ns forge.app
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [forge.core :refer :all])
  (:import (com.badlogic.gdx ApplicationAdapter)
           (com.badlogic.gdx.backends.lwjgl3 Lwjgl3Application Lwjgl3ApplicationConfiguration)
           (com.badlogic.gdx.utils SharedLibraryLoader)
           (java.awt Taskbar Toolkit)
           (org.lwjgl.system Configuration)))

(defn -main []
  (let [{:keys [requires
                dock-icon
                mac-os
                title fps width height
                components]} (-> "app.edn" io/resource slurp edn/read-string)]
    (run! require requires)
    (.setIconImage (Taskbar/getTaskbar)
                   (.getImage (Toolkit/getDefaultToolkit)
                              (io/resource dock-icon)))
    (when SharedLibraryLoader/isMac
      (.set Configuration/GLFW_LIBRARY_NAME  "glfw_async")
      (.set Configuration/GLFW_CHECK_THREAD0 false))
    (Lwjgl3Application. (proxy [ApplicationAdapter] []
                          (create  []    (run! app-create          components))
                          (dispose []    (run! app-destroy         components))
                          (render  []    (run! app-render          components))
                          (resize  [w h] (run! #(app-resize % w h) components)))
                        (doto (Lwjgl3ApplicationConfiguration.)
                          (.setTitle title)
                          (.setForegroundFPS fps)
                          (.setWindowedMode width height)))))
