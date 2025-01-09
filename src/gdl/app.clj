(ns gdl.app
  (:require [clojure.gdx :as gdx]
            [clojure.java.io :as io])
  (:import (com.badlogic.gdx Application
                             ApplicationAdapter)
           (com.badlogic.gdx.backends.lwjgl3 Lwjgl3Application
                                             Lwjgl3ApplicationConfiguration)
           (com.badlogic.gdx.utils SharedLibraryLoader)
           (java.awt Taskbar
                     Toolkit)
           (org.lwjgl.system Configuration)))

(defprotocol Listener
  (create [_ context])
  (dispose [_])
  (render [_])
  (resize [_ width height]))

(defn start [config listener]
  (when-let [icon (:icon config)]
    (.setIconImage (Taskbar/getTaskbar)
                   (.getImage (Toolkit/getDefaultToolkit)
                              (io/resource icon))))
  (when (and SharedLibraryLoader/isMac
             (:glfw-async-on-mac-osx? config))
    (.set Configuration/GLFW_LIBRARY_NAME "glfw_async"))
  (Lwjgl3Application. (proxy [ApplicationAdapter] []
                        (create []
                          (create listener (gdx/context)))
                        (dispose []
                          (dispose listener))
                        (render []
                          (render listener))
                        (resize [width height]
                          (resize listener width height)))
                      (doto (Lwjgl3ApplicationConfiguration.)
                        (.setTitle (:title config))
                        (.setWindowedMode (:width config) (:height config))
                        (.setForegroundFPS (:fps config)))))

(defn post-runnable [context runnable]
  (Application/.postRunnable (:clojure.gdx/app context) runnable))
