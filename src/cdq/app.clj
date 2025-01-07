(ns cdq.app
  "Application starting point."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [cdq.app.listener :as listener])
  (:import (com.badlogic.gdx ApplicationAdapter)
           (com.badlogic.gdx.backends.lwjgl3 Lwjgl3Application Lwjgl3ApplicationConfiguration)
           (com.badlogic.gdx.utils SharedLibraryLoader)
           (java.awt Taskbar Toolkit)
           (org.lwjgl.system Configuration))
  (:gen-class))

(def state
 "Application state in an `atom`. Do not `swap!`, `reset!`, etc. this unless for debugging purposes."
 (atom nil))

(defn -main
  "Reads `gdl.app.edn` configuration from resource path and starts the game.

  It should contain:

  * `:icon` - a string referencing a resource to set as the taskbar icon
  * `:title` - window title
  * `:window-width` - window width in pixel
  * `:window-height` - window height in pixel
  * `:fps` - the frames per second.
  * `:context` - parameters for `cdq.app.listener/create` function."
  []
  (let [config (-> "gdl.app.edn" io/resource slurp edn/read-string)]
    (.setIconImage (Taskbar/getTaskbar)
                   (.getImage (Toolkit/getDefaultToolkit)
                              (io/resource (:icon config))))
    (when SharedLibraryLoader/isMac
      (.set Configuration/GLFW_LIBRARY_NAME "glfw_async")
      (.set Configuration/GLFW_CHECK_THREAD0 false))
    (Lwjgl3Application. (proxy [ApplicationAdapter] []
                          (create []
                            (reset! state (listener/create (:context config))))

                          (dispose []
                            (listener/dispose @state))

                          (render []
                            (swap! state listener/render))

                          (resize [width height]
                            (listener/resize @state width height)))
                        (doto (Lwjgl3ApplicationConfiguration.)
                          (.setTitle (:title config))
                          (.setWindowedMode (:window-width config)
                                            (:window-height config))
                          (.setForegroundFPS (:fps config))))))
