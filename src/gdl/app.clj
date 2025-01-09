(ns gdl.app
  (:require [clojure.edn :as edn]
            [clojure.gdx :as gdx]
            [clojure.java.io :as io]
            [gdl.utils :as utils])
  (:import (com.badlogic.gdx Application
                             ApplicationAdapter)
           (com.badlogic.gdx.backends.lwjgl3 Lwjgl3Application
                                             Lwjgl3ApplicationConfiguration)
           (com.badlogic.gdx.utils SharedLibraryLoader)
           (java.awt Taskbar
                     Toolkit)
           (org.lwjgl.system Configuration)))

(defprotocol Listener
  (create  [_ config])
  (dispose [_])
  (render  [_])
  (resize  [_ width height]))

(defn start* [state listener config]
  (when-let [icon (:icon config)]
    (.setIconImage (Taskbar/getTaskbar)
                   (.getImage (Toolkit/getDefaultToolkit)
                              (io/resource icon))))
  (when (and SharedLibraryLoader/isMac
             (:glfw-async-on-mac-osx? config))
    (.set Configuration/GLFW_LIBRARY_NAME "glfw_async"))
  (Lwjgl3Application. (proxy [ApplicationAdapter] []
                        (create []
                          (reset! state (create (utils/safe-merge listener (gdx/context))
                                                config)))
                        (dispose []
                          (swap! state dispose))
                        (render []
                          (swap! state render))
                        (resize [width height]
                          (swap! state resize width height)))
                      (doto (Lwjgl3ApplicationConfiguration.)
                        (.setTitle (:title config))
                        (.setWindowedMode (:width config) (:height config))
                        (.setForegroundFPS (:fps config)))))

(defn start
  ([state listener]
   (start state listener "config.edn"))
  ([state listener edn-config]
   (start* state
           listener
           (-> edn-config io/resource slurp edn/read-string))))

(defn post-runnable [context runnable]
  (Application/.postRunnable (:clojure.gdx/app context) runnable))
