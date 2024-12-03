(ns forge.start
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [forge.core :refer :all])
  (:import (com.badlogic.gdx ApplicationAdapter)
           (com.badlogic.gdx.backends.lwjgl3 Lwjgl3Application Lwjgl3ApplicationConfiguration)
           (java.awt Taskbar Toolkit)
           (org.lwjgl.system Configuration)))

(defn- set-dock-icon [path]
  (.setIconImage (Taskbar/getTaskbar)
                 (.getImage (Toolkit/getDefaultToolkit)
                            (io/resource path))))

(defn- set-glfw-config [{:keys [library-name check-thread0]}]
  (.set Configuration/GLFW_LIBRARY_NAME library-name)
  (.set Configuration/GLFW_CHECK_THREAD0 check-thread0))

(defn- start-app [app-listener {:keys [title fps width height]}]
  (Lwjgl3Application. app-listener
                      (doto (Lwjgl3ApplicationConfiguration.)
                        (.setTitle title)
                        (.setForegroundFPS fps)
                        (.setWindowedMode width height))))

(defn -main []
  (let [{:keys [requires
                dock-icon
                glfw
                lwjgl3
                components]} (-> "app.edn" io/resource slurp edn/read-string)]
    (run! require requires)
    (set-dock-icon dock-icon)
    (set-glfw-config glfw)
    (start-app (proxy [ApplicationAdapter] []
                 (create  []     (run! app-create          components))
                 (dispose []     (run! app-dispose         components))
                 (render  []     (run! app-render          components))
                 (resize  [w h]  (run! #(app-resize % w h) components)))
               lwjgl3)))
