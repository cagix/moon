(ns gdl.application
  (:require [clojure.java.io :as io])
  (:import (com.badlogic.gdx ApplicationAdapter Gdx)
           (com.badlogic.gdx.backends.lwjgl3 Lwjgl3Application Lwjgl3ApplicationConfiguration)
           (com.badlogic.gdx.utils SharedLibraryLoader Os)
           (java.awt Taskbar Toolkit)
           (org.lwjgl.system Configuration)))

(defprotocol Disposable
  (dispose! [_]))

(extend-type com.badlogic.gdx.utils.Disposable
  Disposable
  (dispose! [this]
    (.dispose this)))

(def state (atom nil))

(defn post-runnable [f]
  (.postRunnable Gdx/app (fn [] (f @state))))

(defn start! [config create-context render-pipeline on-resize]
  (when (= SharedLibraryLoader/os Os/MacOsX)
    (.setIconImage (Taskbar/getTaskbar)
                   (.getImage (Toolkit/getDefaultToolkit)
                              (io/resource (:dock-icon (:mac-os config)))))
    (.set Configuration/GLFW_LIBRARY_NAME "glfw_async"))
  (Lwjgl3Application. (proxy [ApplicationAdapter] []
                        (create []
                          (reset! state (create-context)))

                        (dispose []
                          (doseq [[k value] @state]
                            (if (satisfies? Disposable value)
                              (do
                               #_(println "Disposing:" k)
                               (dispose! value))
                              #_(println "Not Disposable: " k ))))

                        (render []
                          (swap! state (fn [context]
                                         (reduce (fn [context f]
                                                   (f context))
                                                 context
                                                 render-pipeline))))

                        (resize [width height]
                          (on-resize @state width height)))
                      (doto (Lwjgl3ApplicationConfiguration.)
                        (.setTitle (:title config))
                        (.setWindowedMode (:width  (:windowed-mode config))
                                          (:height (:windowed-mode config)))
                        (.setForegroundFPS (:foreground-fps config)))))
