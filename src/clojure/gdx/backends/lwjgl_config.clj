(ns clojure.gdx.backends.lwjgl-config
  (:import (com.badlogic.gdx.backends.lwjgl3 Lwjgl3ApplicationConfiguration
                                             Lwjgl3ApplicationConfiguration$GLEmulation)))

(defmulti ^:private set-option!
  (fn [k _v _config]
    k))

(defn create [options]
  (let [config (Lwjgl3ApplicationConfiguration.)]
    (doseq [[k v] options]
      (set-option! k v config))
    config))

(defn- k->glversion [gl-version]
  (case gl-version
    :angle-gles20 Lwjgl3ApplicationConfiguration$GLEmulation/ANGLE_GLES20
    :gl20         Lwjgl3ApplicationConfiguration$GLEmulation/GL20
    :gl30         Lwjgl3ApplicationConfiguration$GLEmulation/GL30
    :gl31         Lwjgl3ApplicationConfiguration$GLEmulation/GL31
    :gl32         Lwjgl3ApplicationConfiguration$GLEmulation/GL32))

(defmethod set-option! :initial-visible? [_ v config]
  (.setInitialVisible config (boolean v)))

(defmethod set-option! :disable-audio? [_ v config]
  (.disableAudio      config (boolean v)))

(defmethod set-option! :max-net-threads [_ v config]
  (.setMaxNetThreads  config (int v)))

(defmethod set-option! :audio [_ v config]
  (.setAudioConfig    config
                   (int (:simultaneousSources v))
                   (int (:buffer-size         v))
                   (int (:buffer-count        v))))

(defmethod set-option! :opengl-emulation [_ v config]
  (.setOpenGLEmulation config
                       (k->glversion (:gl-version v))
                       (int (:gles-3-major-version v))
                       (int (:gles-3-minor-version v))))

(defmethod set-option! :backbuffer [_ v config]
  (.setBackBufferConfig config
                        (int (:r       v))
                        (int (:g       v))
                        (int (:b       v))
                        (int (:a       v))
                        (int (:depth   v))
                        (int (:stencil v))
                        (int (:samples v))))

(defmethod set-option! :transparent-framebuffer [_ v config]
  (.setTransparentFramebuffer config (boolean v)))

(defmethod set-option! :idle-fps [_ v config]
  (.setIdleFPS config (int v)))

(defmethod set-option! :foreground-fps [_ v config]
  (.setForegroundFPS config (int v)))

(defmethod set-option! :pause-when-minimized? [_ v config]
  (.setPauseWhenMinimized config (boolean v)))

(defmethod set-option! :pause-when-lost-focus? [_ v config]
  (.setPauseWhenLostFocus config (boolean v)))

#_(defmethod set-option! :preferences [_ v config]
  (.setPreferencesConfig config
                         (str (:directory v))
                         ; com.badlogic.gdx.Files.FileType
                         (k->filetype (:filetype v))))

#_(defmethod set-option! :hdpi-mode [_ v config]
  ; com.badlogic.gdx.graphics.glutils.HdpiMode
  (.setHdpiMode config (k->hdpi-mode v)))

#_(defmethod set-option! :gl-debug-output? [_ v config]
  (.enableGLDebugOutput config
                        (boolean (:enable? v))
                        (->PrintStream (:debug-output-stream v))))

(defmethod set-option! :title [_ v config]
  (.setTitle config (str v)))

(defmethod set-option! :windowed-mode [_ v config]
  (.setWindowedMode config
                    (int (:width v))
                    (int (:height v))))

(defmethod set-option! :resizable? [_ v config]
  (.setResizable config (boolean v)))

(defmethod set-option! :decorated? [_ v config]
  (.setDecorated config (boolean v)))

(defmethod set-option! :maximized? [_ v config]
  (.setMaximized config (boolean v)))

#_(defmethod set-option! :maximized-monitor [_ v config]
  (.setMaximizedMonitor config ()))

#_(defmethod set-option! :auto-iconify? [_ v config]
  (.setAutoIconify config ()))

#_(defmethod set-option! :window-position [_ v config]
  (.setWindowPosition config ()))

#_(defmethod set-option! :window-size-limits? [_ v config]
  (.setWindowSizeLimits config ()))

#_(defmethod set-option! :window-icon [_ v config]
  (.setWindowIcon config ())) ; TODO multiple options

#_(defmethod set-option! :window-listener [_ v config]
  (.setWindowListener config ()))

#_(defmethod set-option! :fullscreen-mode [_ v config]
  (.setFullscreenMode config ()))

#_(defmethod set-option! :title [_ v config]
  (.setTitle config (str v)))

#_(defmethod set-option! :initial-background-color [_ v config]
  (.setInitialBackgroundColorer config (->munge-color v)))

#_(defmethod set-option! :vsync? [_ v config]
  (.useVsync config (boolean v)))
