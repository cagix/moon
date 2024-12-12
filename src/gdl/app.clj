(ns gdl.app
  (:require [clojure.java.io :as io])
  (:import (com.badlogic.gdx ApplicationAdapter Gdx)
           (com.badlogic.gdx.backends.lwjgl3 Lwjgl3Application
                                             Lwjgl3ApplicationConfiguration)
           (com.badlogic.gdx.utils SharedLibraryLoader)
           (java.awt Taskbar Toolkit)
           (org.lwjgl.system Configuration)))

(defn- set-taskbar-icon
  "On MacOs this is the dock-icon."
  [icon]
  (.setIconImage (Taskbar/getTaskbar)
                 (.getImage (Toolkit/getDefaultToolkit)
                            (io/resource icon))))

(defprotocol Listener
  (create  [_])
  (dispose [_])
  (render  [_])
  (resize  [_ w h]))

(defn start [{:keys [title fps width height taskbar-icon]} listener]
  (when taskbar-icon
    (set-taskbar-icon taskbar-icon))
  (when SharedLibraryLoader/isMac
    (.set Configuration/GLFW_LIBRARY_NAME "glfw_async")
    (.set Configuration/GLFW_CHECK_THREAD0 false))
  (Lwjgl3Application. (proxy [ApplicationAdapter] []
                        (create  []    (create  listener))
                        (dispose []    (dispose listener))
                        (render  []    (render  listener))
                        (resize  [w h] (resize  listener w h)))
                      (doto (Lwjgl3ApplicationConfiguration.)
                        (.setTitle title)
                        (.setForegroundFPS fps)
                        (.setWindowedMode width height))))

(defn exit []
  (.exit Gdx/app))

(defmacro post-runnable [& exprs]
  `(.postRunnable Gdx/app (fn [] ~@exprs)))
