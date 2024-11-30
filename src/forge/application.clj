(ns forge.application
  (:refer-clojure :exclude [do])
  (:require [clojure.java.io :as io])
  (:import (com.badlogic.gdx ApplicationAdapter Gdx)
           (com.badlogic.gdx.backends.lwjgl3 Lwjgl3Application Lwjgl3ApplicationConfiguration)
           (com.badlogic.gdx.utils SharedLibraryLoader)
           (java.awt Taskbar Toolkit)
           (org.lwjgl.system Configuration)))

(defprotocol Listener
  (create [_])
  (dispose [_])
  (render [_])
  (resize [_ w h]))

(defn- set-dock-icon [image-resource]
  (.setIconImage (Taskbar/getTaskbar)
                 (.getImage (Toolkit/getDefaultToolkit)
                            (io/resource image-resource))))

(defn- lwjgl3-config [{:keys [title fps width height]}]
  (doto (Lwjgl3ApplicationConfiguration.)
    (.setTitle title)
    (.setForegroundFPS fps)
    (.setWindowedMode width height)))

(defn- configure-glfw-for-mac []
  (.set Configuration/GLFW_LIBRARY_NAME "glfw_async")
  (.set Configuration/GLFW_CHECK_THREAD0 false))

(defn start [{:keys [dock-icon title fps width height]} listener]
  (set-dock-icon dock-icon)
  (when SharedLibraryLoader/isMac
    (configure-glfw-for-mac))
  (Lwjgl3Application. (proxy [ApplicationAdapter] []
                        (create []
                          (create listener))

                        (dispose []
                          (dispose listener))

                        (render []
                          (render listener))

                        (resize [w h]
                          (resize listener w h)))
                      (lwjgl3-config {:title title
                                      :fps fps
                                      :width width
                                      :height height})))

(defn exit []
  (.exit Gdx/app))

(defmacro do [& exprs]
  `(.postRunnable Gdx/app (fn [] ~@exprs)))
