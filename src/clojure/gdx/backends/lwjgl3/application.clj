(ns clojure.gdx.backends.lwjgl3.application
  (:require [clojure.application :as app]
            [clojure.gdx :as gdx]
            [clojure.java.io :as io])
  (:import (java.awt Taskbar Toolkit)
           (com.badlogic.gdx ApplicationAdapter Gdx)
           (com.badlogic.gdx.backends.lwjgl3 Lwjgl3Application
                                             Lwjgl3ApplicationConfiguration)
           (com.badlogic.gdx.utils SharedLibraryLoader)
           (org.lwjgl.system Configuration)))

; after creating this  map (set! (.app Gdx) nil) ??  & others ?!

(defn create [listener config]
  (.setIconImage (Taskbar/getTaskbar)
                 (.getImage (Toolkit/getDefaultToolkit)
                            (io/resource (:icon config))))
  (when SharedLibraryLoader/isMac
    (.set Configuration/GLFW_LIBRARY_NAME "glfw_async")
    #_(.set Configuration/GLFW_CHECK_THREAD0 false))
  (Lwjgl3Application. (proxy [ApplicationAdapter] []
                        (create []
                          (app/create listener (gdx/context)))
                        (dispose []
                          (app/dispose listener))
                        (pause []
                          (app/pause listener))
                        (render []
                          (app/render listener))
                        (resize [width height]
                          (app/resize listener width height))
                        (resume []
                          (app/resume listener)))
                      (doto (Lwjgl3ApplicationConfiguration.)
                        (.setTitle (:title config))
                        (.setWindowedMode (:window-width config)
                                          (:window-height config))
                        (.setForegroundFPS (:fps config)))))
