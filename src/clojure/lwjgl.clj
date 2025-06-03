(ns clojure.lwjgl
  (:require [clojure.gdx.lwjgl.interop :as i]
            [clojure.java.io :as io]
            [clojure.utils :as utils])
  (:import (com.badlogic.gdx ApplicationAdapter)
           (com.badlogic.gdx.backends.lwjgl3 Lwjgl3Application
                                             Lwjgl3ApplicationConfiguration
                                             Lwjgl3WindowConfiguration)
           (com.badlogic.gdx.utils SharedLibraryLoader
                                   Os)
           (java.awt Taskbar
                     Toolkit)
           (org.lwjgl.system Configuration)))

(defn set-glfw-async! []
  (.set Configuration/GLFW_LIBRARY_NAME "glfw_async"))

(defn set-taskbar-icon! [io-resource]
  (.setIconImage (Taskbar/getTaskbar)
                 (.getImage (Toolkit/getDefaultToolkit)
                            (io/resource io-resource))))

(defn display-mode
  ([monitor]
   (i/display-mode->map (Lwjgl3ApplicationConfiguration/getDisplayMode (i/map->monitor monitor))))
  ([]
   (i/display-mode->map (Lwjgl3ApplicationConfiguration/getDisplayMode))))

(defn display-modes
  "The available display-modes of the primary or the given monitor."
  ([monitor]
   (map i/display-mode->map (Lwjgl3ApplicationConfiguration/getDisplayModes (i/map->monitor monitor))))
  ([]
   (map i/display-mode->map (Lwjgl3ApplicationConfiguration/getDisplayModes))))

(defn primary-monitor
  "the primary monitor."
  []
  (i/monitor->map (Lwjgl3ApplicationConfiguration/getPrimaryMonitor)))

(defn monitors
  "The connected monitors."
  []
  (map i/monitor->map (Lwjgl3ApplicationConfiguration/getMonitors)))

(defn window
  "Creates a new Lwjgl3Window using the provided listener and Lwjgl3WindowConfiguration. This function only just instantiates a Lwjgl3Window and returns immediately. The actual window creation is postponed with Application.postRunnable(Runnable) until after all existing windows are updated."
  [application listener config]
  (Lwjgl3Application/.newWindow application
                                listener
                                (let [obj (Lwjgl3WindowConfiguration.)]
                                  (doseq [[k v] config]
                                    (i/set-window-config-key! obj k v))
                                  obj)))

(defn set-gl-debug-message-control
  "Enables or disables GL debug messages for the specified severity level. Returns false if the severity level could not be set (e.g. the NOTIFICATION level is not supported by the ARB and AMD extensions). See Lwjgl3ApplicationConfiguration.enableGLDebugOutput(boolean, PrintStream)"
  [severity enabled?]
  (Lwjgl3Application/setGLDebugMessageControl (i/k->gl-debug-message-severity severity)
                                              (boolean enabled?)))

(defn start-application! [listener config]
  (Lwjgl3Application. listener
                      (let [obj (Lwjgl3ApplicationConfiguration.)]
                        (doseq [[k v] config]
                          (i/set-application-config-key! obj k v))
                        obj)))

(defn -main [config-path]
  (let [config (utils/load-edn-config config-path)]
    (when-let [mac-settings (:mac-os (::config config))]
      (when (= SharedLibraryLoader/os Os/MacOsX)
        (let [{:keys [glfw-async?
                      dock-icon]} mac-settings]
          (when glfw-async?
            (set-glfw-async!))
          (when dock-icon
            (set-taskbar-icon! dock-icon)))))
    (start-application! (proxy [ApplicationAdapter] []
                          (create []
                            ((::create! config) config))

                          (dispose []
                            ((::dispose! config)))

                          (render []
                            ((::render! config)))

                          (resize [width height]
                            ((::resize! config) width height)))
                        (dissoc (::config config) :mac-os))))
