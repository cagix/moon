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

(defn- set-window-config-key! [^Lwjgl3WindowConfiguration object k v]
  (case k
    :initial-visible? (.setInitialVisible object (boolean v))
    :windowed-mode   (.setWindowedMode object
                                       (int (:width v))
                                       (int (:height v)))
    :resizable? (.setResizable object (boolean v))
    :decorated? (.setDecorated object (boolean v))
    :maximized? (.setMaximized object (boolean v))
    :maximized-monitor (.setMaximizedMonitor object (i/map->monitor v))
    :auto-iconify? (.setAutoIconify object (boolean v))
    :window-position (.setWindowPosition object
                                         (int (:x v))
                                         (int (:y v)))
    :window-size-limits (.setWindowSizeLimits object
                                              (int (:min-width  v))
                                              (int (:min-height v))
                                              (int (:max-width  v))
                                              (int (:max-height v)))
    :window-icons (.setWindowIcon object
                                  ; filetype
                                  ; array of string of file icons
                                  )

    :window-listener (.setWindowListener object
                                         ; Lwjgl3WindowListener v
                                         )

    :initial-background-color (.setInitialBackgroundColorer object #_(->munge-color v))

    :fullscreen-mode (.setFullscreenMode object (i/map->display-mode v))
    :title (.setTitle object (str v))
    :vsync? (.useVsync object (boolean v))))

(defn- set-glfw-async! []
  (.set Configuration/GLFW_LIBRARY_NAME "glfw_async"))

(defn- set-taskbar-icon! [io-resource]
  (.setIconImage (Taskbar/getTaskbar)
                 (.getImage (Toolkit/getDefaultToolkit)
                            (io/resource io-resource))))

(defn- set-application-config-key! [^Lwjgl3ApplicationConfiguration object k v]
  (case k
    :mac-os (when (= SharedLibraryLoader/os Os/MacOsX)
              (let [{:keys [glfw-async?
                            dock-icon]} v]
                (when glfw-async?
                  (set-glfw-async!))
                (when dock-icon
                  (set-taskbar-icon! dock-icon))))
    :audio (.setAudioConfig object
                            (int (:simultaneous-sources v))
                            (int (:buffer-size         v))
                            (int (:buffer-count        v)))
    :disable-audio? (.disableAudio object (boolean v))
    :max-net-threads (.setMaxNetThreads object (int v))
    :opengl-emulation (.setOpenGLEmulation object
                                           (i/k->glversion (:gl-version v))
                                           (int (:gles-3-major-version v))
                                           (int (:gles-3-minor-version v)))
    :backbuffer (.setBackBufferConfig object
                                      (int (:r       v))
                                      (int (:g       v))
                                      (int (:b       v))
                                      (int (:a       v))
                                      (int (:depth   v))
                                      (int (:stencil v))
                                      (int (:samples v)))
    :transparent-framebuffer (.setTransparentFramebuffer object (boolean v))
    :idle-fps (.setIdleFPS object (int v))
    :foreground-fps (.setForegroundFPS object (int v))
    :pause-when-minimized? (.setPauseWhenMinimized object (boolean v))
    :pause-when-lost-focus? (.setPauseWhenLostFocus object (boolean v))

    ; String preferencesDirectory, Files.FileType preferencesFileType
    #_(defmethod set-option! :preferences [object _ v]
        (.setPreferencesConfig object
                               (str (:directory v))
                               ; com.badlogic.gdx.Files.FileType
                               (k->filetype (:filetype v))))

    ; com.badlogic.gdx.graphics.glutils.HdpiMode/ 'Logical' / 'Pixels'
    #_(defmethod set-option! :hdpi-mode [object _ v]
        ; com.badlogic.gdx.graphics.glutils.HdpiMode
        (.setHdpiMode object (k->hdpi-mode v)))

    #_(defmethod set-option! :gl-debug-output? [object _ v]
        (.enableGLDebugOutput object
                              (boolean (:enable? v))
                              (->PrintStream (:debug-output-stream v))))

    (set-window-config-key! object k v)))

(defn- create-application-config [config]
  (let [obj (Lwjgl3ApplicationConfiguration.)]
    (doseq [[k v] config]
      (set-application-config-key! obj k v))
    obj))

(defn- create-window-config [config]
  (let [obj (Lwjgl3WindowConfiguration.)]
    (doseq [[k v] config]
      (set-window-config-key! obj k v))
    obj))

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
                                (create-window-config config)))

(defn set-gl-debug-message-control
  "Enables or disables GL debug messages for the specified severity level. Returns false if the severity level could not be set (e.g. the NOTIFICATION level is not supported by the ARB and AMD extensions). See Lwjgl3ApplicationConfiguration.enableGLDebugOutput(boolean, PrintStream)"
  [severity enabled?]
  (Lwjgl3Application/setGLDebugMessageControl (i/k->gl-debug-message-severity severity)
                                              (boolean enabled?)))

(defn -main [config-path]
  (let [config (utils/load-edn-config config-path)]
    (Lwjgl3Application. (proxy [ApplicationAdapter] []
                          (create []
                            ((::create! config) config))

                          (dispose []
                            ((::dispose! config)))

                          (render []
                            ((::render! config)))

                          (resize [width height]
                            ((::resize! config) width height)))
                        (create-application-config (::config config)))))
