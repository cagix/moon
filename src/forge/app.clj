(ns forge.app
  (:require [forge.base :refer :all])
  (:import (com.badlogic.gdx ApplicationAdapter)
           (com.badlogic.gdx.backends.lwjgl3 Lwjgl3Application Lwjgl3ApplicationConfiguration)
           (java.awt Taskbar Toolkit)
           (org.lwjgl.system Configuration)))

(defn set-dock-icon [path]
  (.setIconImage (Taskbar/getTaskbar)
                 (.getImage (Toolkit/getDefaultToolkit)
                            (io-resource path))))

(defn set-glfw-config [{:keys [library-name check-thread0]}]
  (.set Configuration/GLFW_LIBRARY_NAME library-name)
  (.set Configuration/GLFW_CHECK_THREAD0 check-thread0))

(defn lwjgl3-config [{:keys [title fps width height]}]
  (doto (Lwjgl3ApplicationConfiguration.)
    (.setTitle title)
    (.setForegroundFPS fps)
    (.setWindowedMode width height)))

(defn lwjgl3-app [application lwjgl3-config]
  (Lwjgl3Application. application lwjgl3-config))

(defn components-app [components]
  (proxy [ApplicationAdapter] []
    (create []
      (run! app-create components))

    (dispose []
      (run! app-dispose components))

    (render []
      (run! app-render components))

    (resize [w h]
      (run! #(app-resize % w h) components))))
