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

(defn post-runnable [app runnable]
  (Application/.postRunnable app runnable))

(defn set-icon! [icon-resource]
  (.setIconImage (Taskbar/getTaskbar)
                 (.getImage (Toolkit/getDefaultToolkit)
                            (io/resource icon-resource))))

(def mac-osx? SharedLibraryLoader/isMac)

(defn set-glfw-to-async! []
  (.set Configuration/GLFW_LIBRARY_NAME "glfw_async"))
