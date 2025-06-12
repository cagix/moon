(ns gdl.application
  (:require [clojure.gdx :as gdx]
            [gdl.input :as input]
            [gdl.utils.disposable]
            [gdx.utils.shared-library-loader :as shared-library-loader])
  (:import (com.badlogic.gdx ApplicationAdapter)
           (com.badlogic.gdx.backends.lwjgl3 Lwjgl3Application
                                             Lwjgl3ApplicationConfiguration
                                             Lwjgl3ApplicationConfiguration$GLEmulation
                                             Lwjgl3WindowConfiguration)))

(extend-type com.badlogic.gdx.utils.Disposable
  gdl.utils.disposable/Disposable
  (dispose! [object]
    (.dispose object)))

(extend-type com.badlogic.gdx.Input
  gdl.input/Input
  (button-just-pressed? [this button]
    (.isButtonJustPressed this (gdx/k->Input$Buttons button)))

  (key-pressed? [this key]
    (.isKeyPressed this (gdx/k->Input$Keys key)))

  (key-just-pressed? [this key]
    (.isKeyJustPressed this (gdx/k->Input$Keys key)))

  (mouse-position [this]
    [(.getX this)
     (.getY this)]))

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

(defn- execute! [[f params]]
  ;(println "execute!" [f params])
  (f params))

; 0. reda config chere
; 1. pass Gdx state
; 2. txs start inside cereate/dispoes/render/resize only ?
; 3. state here ?
; 4. inside graphics again txs !?
(defn start!
  [os-config
   lwjgl3-config
   {:keys [create! dispose! render! resize!]}]
  (run! execute! (get os-config (shared-library-loader/operating-system)))
  (Lwjgl3Application. (proxy [ApplicationAdapter] []
                        (create []
                          (create! {:ctx/input (gdx/input)}))
                        (dispose []
                          (dispose!))
                        (render []
                          (render!))
                        (resize [width height]
                          (resize! width height)))
                      (let [obj (Lwjgl3ApplicationConfiguration.)]
                        (doseq [[k v] lwjgl3-config]
                          (set-application-config-key! obj k v))
                        obj)))
