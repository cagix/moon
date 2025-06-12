(ns gdl.application
  (:require [clojure.java.io :as io])
  (:import (com.badlogic.gdx ApplicationAdapter)
           (com.badlogic.gdx.backends.lwjgl3 Lwjgl3Application
                                             Lwjgl3ApplicationConfiguration
                                             Lwjgl3ApplicationConfiguration$GLEmulation
                                             Lwjgl3WindowConfiguration)
           (com.badlogic.gdx.utils SharedLibraryLoader
                                   Os)
           (java.awt Taskbar
                     Toolkit)
           (org.lwjgl.system Configuration)))

(defn- k->glversion [gl-version]
  (case gl-version
    :gl-emulation/angle-gles20 Lwjgl3ApplicationConfiguration$GLEmulation/ANGLE_GLES20
    :gl-emulation/gl20         Lwjgl3ApplicationConfiguration$GLEmulation/GL20
    :gl-emulation/gl30         Lwjgl3ApplicationConfiguration$GLEmulation/GL30
    :gl-emulation/gl31         Lwjgl3ApplicationConfiguration$GLEmulation/GL31
    :gl-emulation/gl32         Lwjgl3ApplicationConfiguration$GLEmulation/GL32))

(defn- set-window-config-key! [^Lwjgl3WindowConfiguration object k v]
  (case k
    :initial-visible? (.setInitialVisible object (boolean v))
    :windowed-mode   (.setWindowedMode object
                                       (int (:width v))
                                       (int (:height v)))
    :resizable? (.setResizable object (boolean v))
    :decorated? (.setDecorated object (boolean v))
    :maximized? (.setMaximized object (boolean v))
    ;:maximized-monitor (.setMaximizedMonitor object (map->monitor v))
    :auto-iconify? (.setAutoIconify object (boolean v))
    :window-position (.setWindowPosition object
                                         (int (:x v))
                                         (int (:y v)))
    :window-size-limits (.setWindowSizeLimits object
                                              (int (:min-width  v))
                                              (int (:min-height v))
                                              (int (:max-width  v))
                                              (int (:max-height v)))
    :window-icons (.setWindowIcon object ; TODO
                                  ; filetype
                                  ; array of string of file icons
                                  )
    :window-listener (.setWindowListener object
                                         ; Lwjgl3WindowListener v
                                         )
    :initial-background-color (.setInitialBackgroundColor object v)
    ;:fullscreen-mode (.setFullscreenMode object (map->display-mode v))
    :title (.setTitle object (str v))
    :vsync? (.useVsync object (boolean v))))

(defn- set-application-config-key! [^Lwjgl3ApplicationConfiguration object k v]
  (case k
    :audio (.setAudioConfig object
                            (int (:simultaneous-sources v))
                            (int (:buffer-size         v))
                            (int (:buffer-count        v)))
    :disable-audio? (.disableAudio object (boolean v))
    :max-net-threads (.setMaxNetThreads object (int v))
    :opengl-emulation (.setOpenGLEmulation object
                                           (k->glversion (:gl-version v))
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

(let [mapping {Os/Android :android
               Os/IOS     :ios
               Os/Linux   :linux
               Os/MacOsX  :mac
               Os/Windows :windows}]
  (defn- operating-system []
    (get mapping SharedLibraryLoader/os)))

(defn start!
  [os-config
   lwjgl3-config
   {:keys [create! dispose! render! resize!]}]
  (when (= (operating-system) :mac)
    (let [{:keys [glfw-async?
                  dock-icon]} (:mac os-config)]
      (when glfw-async?
        (.set Configuration/GLFW_LIBRARY_NAME "glfw_async"))
      (when dock-icon
        (.setIconImage (Taskbar/getTaskbar)
                       (.getImage (Toolkit/getDefaultToolkit)
                                  (io/resource dock-icon))))))
  (Lwjgl3Application. (proxy [ApplicationAdapter] []
                        (create []
                          (when create! (create!)))
                        (dispose []
                          (when dispose! (dispose!)))
                        (render []
                          (when render! (render!)))
                        (resize [width height]
                          (when resize! (resize! width height))))
                      (let [obj (Lwjgl3ApplicationConfiguration.)]
                        (doseq [[k v] lwjgl3-config]
                          (set-application-config-key! obj k v))
                        obj)))
