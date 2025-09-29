(ns com.badlogic.gdx.backends.lwjgl
  (:require [clojure.java.awt :as awt]
            [com.badlogic.gdx.backends.lwjgl.files :as files]
            [com.badlogic.gdx.utils.shared-library-loader :as shared-library-loader])
  (:import (com.badlogic.gdx Application
                             Application$ApplicationType
                             ApplicationLogger
                             Gdx)
           (com.badlogic.gdx.graphics GL20)
           (com.badlogic.gdx.backends.lwjgl DefaultLwjgl3Input
                                            Lwjgl3ApplicationConfiguration
                                            Lwjgl3Cursor
                                            Lwjgl3GL20
                                            Lwjgl3GL30
                                            Lwjgl3GL31
                                            Lwjgl3GL32
                                            Lwjgl3Graphics
                                            Lwjgl3Graphics$Lwjgl3DisplayMode
                                            Lwjgl3NativesLoader
                                            Lwjgl3Net
                                            Lwjgl3Window
                                            Sync)
           (com.badlogic.gdx.backends.lwjgl.audio OpenALLwjgl3Audio)
           (com.badlogic.gdx.backends.lwjgl.audio.mock MockAudio)
           (com.badlogic.gdx.graphics.glutils HdpiMode
                                              GLVersion)
           (com.badlogic.gdx.utils Array
                                   Clipboard
                                   Os
                                   SharedLibraryLoader
                                   GdxRuntimeException)
           (org.lwjgl.glfw GLFW
                           GLFWErrorCallback
                           GLFWWindowIconifyCallback
                           GLFWWindowFocusCallback)
           (org.lwjgl.opengl GL
                             GL11
                             GLUtil
                             GL43
                             KHRDebug
                             ARBDebugOutput
                             AMDDebugOutput)
           (org.lwjgl.system Configuration)))

(defn update-framebuffer-info [^Lwjgl3Graphics this]
  (let [handle (.getWindowHandle (.window this))]
    (GLFW/glfwGetFramebufferSize handle
                                 (.tmpBuffer  this)
                                 (.tmpBuffer2 this))
    (set! (.backBufferWidth  this) (.get (.tmpBuffer  this) 0))
    (set! (.backBufferHeight this) (.get (.tmpBuffer2 this) 0))

    (GLFW/glfwGetWindowSize handle
                            (.tmpBuffer  this)
                            (.tmpBuffer2 this))
    (set! (.logicalWidth  this) (.get (.tmpBuffer  this) 0))
    (set! (.logicalHeight this) (.get (.tmpBuffer2 this) 0))
    (let [config (.cljConfig (.window this))]
      (set! (.bufferFormat this)
            (com.badlogic.gdx.Graphics$BufferFormat. (:r       (:buffer-format config))
                                                     (:g       (:buffer-format config))
                                                     (:b       (:buffer-format config))
                                                     (:a       (:buffer-format config))
                                                     (:depth   (:buffer-format config))
                                                     (:stencil (:buffer-format config))
                                                     (:samples (:buffer-format config))
                                                     false)))))

(defn- create-logger []
  (reify ApplicationLogger
    (log [this tag message]
      (println (str "[" tag "] " message)))

    (log [this tag message exception]
      (println (str "[" tag "] " message))
      (.printStackTrace exception System/out))

    (error [this tag message]
      (binding [*out* *err*]
        (println (str "[" tag "] " message))))
    (error [this tag message exception]
      (binding [*out* *err*]
        (println (str "[" tag "] " message))
        (.printStackTrace exception System/err)))

    (debug [this tag message]
      (println (str "[" tag "] " message)))
    (debug [this tag message exception]
      (println (str "[" tag "] " message))
      (.printStackTrace exception System/out))))

(defn- create-clipboard []
  (reify Clipboard
    (hasContents [_]
      (let [contents (.getContents _)]
        (and contents (not (empty? contents)))))

    (getContents [_]
      (let [graphics ^Lwjgl3Graphics Gdx/graphics
            window-handle (.getWindowHandle (.getWindow graphics))]
        (GLFW/glfwGetClipboardString window-handle)))

    (setContents [_ content]
      (let [graphics ^Lwjgl3Graphics Gdx/graphics
            window-handle (.getWindowHandle (.getWindow graphics))]
        (GLFW/glfwSetClipboardString window-handle content)))))

(defn- set-mac-os-config!
  [{:keys [taskbar-icon]}]
  (when-let [path taskbar-icon]
    (awt/set-taskbar-icon! path)))

(defn- initializeListener [^Lwjgl3Window window]
  (when-not (.listenerInitialized window)
    (.create (.listener window))
    (.resize (.listener window)
             (.getWidth (.graphics window))
             (.getHeight (.graphics window)))
    (set! (.listenerInitialized window) true)))

(defn- make-current! [^Lwjgl3Window window]
  (let [graphics (.graphics window)
        input  (.input window)
        window-handle (.windowHandle window)]
    (set! Gdx/graphics graphics)
    (set! Gdx/gl32 (.getGL32 graphics))
    (set! Gdx/gl31 (if Gdx/gl32
                     Gdx/gl32
                     (.getGL31 graphics)))
    (set! Gdx/gl30 (if Gdx/gl31
                     Gdx/gl31
                     (.getGL30 graphics)))
    (set! Gdx/gl20 (if Gdx/gl30
                     Gdx/gl30
                     (.getGL20 graphics)))
    (set! Gdx/gl Gdx/gl20)
    (set! Gdx/input input)
    (GLFW/glfwMakeContextCurrent window-handle)))

(declare ^:private glVersion
         ^:private glDebugCallback
         ^:private errorCallback)

; Use com.badlogic.gdx.graphics.GL20 constants instead of org.lwjgl.opengl.GL11
; so we don't trigger LWJGL GL11 static init (double GL.create)
; In Java, final int fields are inlined at compile time; in Clojure they trigger class init at runtime
(defn- initiate-gl [use-gles20]
  (if-not use-gles20
    (let [version-string  (GL11/glGetString GL20/GL_VERSION)
          vendor-string   (GL11/glGetString GL20/GL_VENDOR)
          renderer-string (GL11/glGetString GL20/GL_RENDERER)]
      (def glVersion (GLVersion. Application$ApplicationType/Desktop
                                 version-string
                                 vendor-string
                                 renderer-string)))
    (try
     (let [version-str  (str (eval `(org.lwjgl.opengles.GLES20/glGetString ~GL20/GL_VERSION)))
           vendor-str   (str (eval `(org.lwjgl.opengles.GLES20/glGetString ~GL20/GL_VENDOR)))
           renderer-str (str (eval `(org.lwjgl.opengles.GLES20/glGetString ~GL20/GL_RENDERER)))]
       (def glVersion (GLVersion. Application$ApplicationType/Desktop
                                  version-str
                                  vendor-str
                                  renderer-str)))
     (catch Throwable e
       (throw (GdxRuntimeException. "Couldn't get GLES version string." e))))))

(defn- supports-fbo []
  ;; FBO is in core since OpenGL 3.0, see https://www.opengl.org/wiki/Framebuffer_Object
  (or (.isVersionEqualToOrHigher ^GLVersion glVersion 3 0)
      (GLFW/glfwExtensionSupported "GL_EXT_framebuffer_object")
      (GLFW/glfwExtensionSupported "GL_ARB_framebuffer_object")))

#_(defn- setGLDebugMessageControl
  "Enables or disables GL debug messages for the specified severity level. Returns false if the severity level could not be
  * set (e.g. the NOTIFICATION level is not supported by the ARB and AMD extensions)."
  []
  (let [caps (GL/getCapabilities)
        GL_DONT_CARE 0x1100 ; not defined anywhere yet
        enabled false]
    (cond
     (.OpenGL43 caps)
     (GL43/glDebugMessageControl GL_DONT_CARE,
                                 GL_DONT_CARE
                                 GL43/GL_DEBUG_SEVERITY_NOTIFICATION
                                 nil
                                 enabled)

     (.GL_KHR_debug caps)
     (KHRDebug/glDebugMessageControl GL_DONT_CARE,
                                     GL_DONT_CARE,
                                     KHRDebug/GL_DEBUG_SEVERITY_NOTIFICATION,
                                     nil,
                                     enabled)

     :else nil))

  ;	if (caps.GL_ARB_debug_output && severity.arb != -1) {
  ;		ARBDebugOutput.glDebugMessageControlARB(GL_DONT_CARE, GL_DONT_CARE, -1, (IntBuffer)null, enabled);
  ;		return true;
  ;	}

  ;	if (caps.GL_AMD_debug_output && severity.amd != -1) {
  ;		AMDDebugOutput.glDebugMessageEnableAMD(GL_DONT_CARE, -1, (IntBuffer)null, enabled);
  ;		return true;
  ;	}
  )

(defn- createGlfwWindow [glEmulation config sharedContext]
  (GLFW/glfwDefaultWindowHints)
  (GLFW/glfwWindowHint GLFW/GLFW_VISIBLE GLFW/GLFW_FALSE)
  (GLFW/glfwWindowHint GLFW/GLFW_RESIZABLE (if (:windowResizable config) GLFW/GLFW_TRUE GLFW/GLFW_FALSE))
  (GLFW/glfwWindowHint GLFW/GLFW_MAXIMIZED (if (:windowMaximized config) GLFW/GLFW_TRUE GLFW/GLFW_FALSE))
  (GLFW/glfwWindowHint GLFW/GLFW_AUTO_ICONIFY (if (:autoIconify config)  GLFW/GLFW_TRUE GLFW/GLFW_FALSE))

  (GLFW/glfwWindowHint GLFW/GLFW_RED_BITS     (:r       (:buffer-format config)))
  (GLFW/glfwWindowHint GLFW/GLFW_GREEN_BITS   (:g       (:buffer-format config)))
  (GLFW/glfwWindowHint GLFW/GLFW_BLUE_BITS    (:b       (:buffer-format config)))
  (GLFW/glfwWindowHint GLFW/GLFW_ALPHA_BITS   (:a       (:buffer-format config)))
  (GLFW/glfwWindowHint GLFW/GLFW_STENCIL_BITS (:stencil (:buffer-format config)))
  (GLFW/glfwWindowHint GLFW/GLFW_DEPTH_BITS   (:depth   (:buffer-format config)))
  (GLFW/glfwWindowHint GLFW/GLFW_SAMPLES      (:samples (:buffer-format config)))

  (cond
   (#{:GLEmulation/GL30
      :GLEmulation/GL31
      :GLEmulation/GL32} glEmulation)
   (do
    (GLFW/glfwWindowHint GLFW/GLFW_CONTEXT_VERSION_MAJOR (:gles30ContextMajorVersion config))
    (GLFW/glfwWindowHint GLFW/GLFW_CONTEXT_VERSION_MINOR (:gles30ContextMinorVersion config))
    (when (= SharedLibraryLoader/os Os/MacOsX)
      ; hints mandatory on OS X for GL 3.2+ context creation, but fail on Windows if the
      ; WGL_ARB_create_context extension is not available
      ; see: http://www.glfw.org/docs/latest/compat.html
      (GLFW/glfwWindowHint GLFW/GLFW_OPENGL_FORWARD_COMPAT GLFW/GLFW_TRUE)
      (GLFW/glfwWindowHint GLFW/GLFW_OPENGL_PROFILE GLFW/GLFW_OPENGL_CORE_PROFILE)))

   (= glEmulation :GLEmulation/ANGLE_GLES20)
   (do
    (GLFW/glfwWindowHint GLFW/GLFW_CONTEXT_CREATION_API GLFW/GLFW_EGL_CONTEXT_API)
    (GLFW/glfwWindowHint GLFW/GLFW_CLIENT_API GLFW/GLFW_OPENGL_ES_API)
    (GLFW/glfwWindowHint GLFW/GLFW_CONTEXT_VERSION_MAJOR 2)
    (GLFW/glfwWindowHint GLFW/GLFW_CONTEXT_VERSION_MINOR 0)))

  (when (:transparentFramebuffer config)
    (GLFW/glfwWindowHint GLFW/GLFW_TRANSPARENT_FRAMEBUFFER GLFW/GLFW_TRUE))

  (when (:debug config)
    (GLFW/glfwWindowHint GLFW/GLFW_OPENGL_DEBUG_CONTEXT GLFW/GLFW_TRUE))

  (let [window-handle (if (:fullscreenMode config)
                        (do
                         (GLFW/glfwWindowHint GLFW/GLFW_REFRESH_RATE
                                              (-> config
                                                  :fullscreenMode
                                                  .refreshRate))
                         (let [h (GLFW/glfwCreateWindow
                                  (-> config :fullscreenMode .width)
                                  (-> config :fullscreenMode .height)
                                  (:title config)
                                  (-> config :fullscreenMode .getMonitor)
                                  sharedContext)]

                           ; On Ubuntu >= 22.04 with Nvidia GPU drivers and X11 display server there's a bug with EGL Context API
                           ; If the windows creation has failed for this reason try to create it again with the native context
                           (if (and (zero? h)
                                    (= glEmulation
                                       :GLEmulation/ANGLE_GLES20))
                             (do
                              (GLFW/glfwWindowHint GLFW/GLFW_CONTEXT_CREATION_API
                                                   GLFW/GLFW_NATIVE_CONTEXT_API)
                              (GLFW/glfwCreateWindow
                               (-> config :fullscreenMode .width)
                               (-> config :fullscreenMode .height)
                               (:title config)
                               (-> config :fullscreenMode .getMonitor)
                               sharedContext))
                             h)))
                        (do

                         (GLFW/glfwWindowHint GLFW/GLFW_DECORATED
                                              (if (:windowDecorated config)
                                                GLFW/GLFW_TRUE
                                                GLFW/GLFW_FALSE))
                         (let [h (GLFW/glfwCreateWindow
                                  (:windowWidth  config)
                                  (:windowHeight config)
                                  (:title config)
                                  0
                                  sharedContext)]

                           ; On Ubuntu >= 22.04 with Nvidia GPU drivers and X11 display server there's a bug with EGL Context API
                           ; If the windows creation has failed for this reason try to create it again with the native context
                           (if (and (zero? h)
                                    (= glEmulation
                                       :GLEmulation/ANGLE_GLES20))
                             (do
                              (GLFW/glfwWindowHint GLFW/GLFW_CONTEXT_CREATION_API
                                                   GLFW/GLFW_NATIVE_CONTEXT_API)
                              (GLFW/glfwCreateWindow
                               (:windowWidth  config)
                               (:windowHeight config)
                               (:title config)
                               0
                               sharedContext))
                             h))))]

    (when (zero? window-handle)
      (throw (GdxRuntimeException. "Couldn't create window")))

    (Lwjgl3Window/setSizeLimits window-handle
                                (:windowMinWidth  config)
                                (:windowMinHeight config)
                                (:windowMaxWidth  config)
                                (:windowMaxHeight config))

    ;; window position & maximize
    (when (nil? (:fullscreenMode config))
      (when (not= (GLFW/glfwGetPlatform) GLFW/GLFW_PLATFORM_WAYLAND)
        (if (and (= -1 (:windowX config))
                 (= -1 (:windowY config))) ; i.e., center the window
          (let [window-width  (-> (max (:windowWidth config)
                                       (:windowMinWidth config))
                                  (cond-> (> (:windowMaxWidth config) -1)
                                    (min (:windowMaxWidth config))))
                window-height (-> (max (:windowHeight config) (:windowMinHeight config))
                                  (cond-> (> (:windowMaxHeight config) -1)
                                    (min (:windowMaxHeight config))))
                monitor-handle (if (and (:windowMaximized config)
                                        (some? (:maximizedMonitor config)))
                                 (-> config
                                     :maximizedMonitor
                                     .monitorHandle)
                                 (GLFW/glfwGetPrimaryMonitor))
                new-pos (Lwjgl3ApplicationConfiguration/calculateCenteredWindowPosition
                          (Lwjgl3ApplicationConfiguration/toLwjgl3Monitor monitor-handle)
                          window-width
                          window-height)]
            (GLFW/glfwSetWindowPos window-handle (.x new-pos) (.y new-pos)))
          (GLFW/glfwSetWindowPos window-handle
                                 (:windowX config)
                                 (:windowY config))))
      (when (:windowMaximized config)
        (GLFW/glfwMaximizeWindow window-handle)))

    (when (some? (:windowIconPaths config))
      ; TODO fixme wants an array
      #_(Lwjgl3Window/setIcon window-handle
                            (:windowIconPaths config)
                            (:windowIconFileType config)))

    (GLFW/glfwMakeContextCurrent window-handle)
    (GLFW/glfwSwapInterval (if (:vSyncEnabled config) 1 0))
    (if (= glEmulation :GLEmulation/ANGLE_GLES20)
      (try
        (let [gles (Class/forName "org.lwjgl.opengles.GLES")]
          (.invoke (.getMethod gles "createCapabilities" (into-array Class [])) gles (object-array [])))
        (catch Throwable e
          (throw (GdxRuntimeException. "Couldn't initialize GLES" e))))
      (GL/createCapabilities))

    (initiate-gl (= glEmulation :GLEmulation/ANGLE_GLES20))
    (when-not (.isVersionEqualToOrHigher glVersion 2 0)
      (throw (GdxRuntimeException.
               (str "OpenGL 2.0 or higher with the FBO extension is required. OpenGL version: "
                    (.getVersionString glVersion) "\n"
                    (.getDebugVersionString glVersion)))))

    (when (and (not= glEmulation :GLEmulation/ANGLE_GLES20)
               (not (supports-fbo)))
      (throw (GdxRuntimeException.
               (str "OpenGL 2.0 or higher with the FBO extension is required. OpenGL version: "
                    (.getVersionString glVersion)
                    ", FBO extension: false\n"
                    (.getDebugVersionString glVersion)))))

    (when (:debug config)
      (when (= glEmulation :GLEmulation/ANGLE_GLES20)
        (throw (IllegalStateException.
                 "ANGLE currently can't be used with Lwjgl3ApplicationConfiguration#enableGLDebugOutput")))
      (def glDebugCallback (GLUtil/setupDebugMessageCallback (:debugStream config)))
      #_(setGLDebugMessageControl))
    window-handle))

(defn- set-gl-impl [this glEmulation]
  (cond
   (= glEmulation :GLEmulation/GL32)
   (let [impl (Lwjgl3GL32.)]
     (set! (.gl20 this) impl)
     (set! (.gl30 this) impl)
     (set! (.gl31 this) impl)
     (set! (.gl32 this) impl))

   (= glEmulation :GLEmulation/GL31)
   (let [impl (Lwjgl3GL31.)]
     (set! (.gl20 this) impl)
     (set! (.gl30 this) impl)
     (set! (.gl31 this) impl))

   (= glEmulation :GLEmulation/GL30)
   (let [impl (Lwjgl3GL30.)]
     (set! (.gl20 this) impl)
     (set! (.gl30 this) impl))

   (= glEmulation :GLEmulation/GL20)
   (let [impl (Lwjgl3GL20.)]
     (set! (.gl20 this) impl))

   :else
   (let [impl (eval '(com.badlogic.gdx.backends.lwjgl3.angle.Lwjgl3GLES20.))]
     (set! (.gl20 this) impl))))

(defn- create-graphics [window glEmulation]
  (let [this (Lwjgl3Graphics.)]
    (set! (.window this) window)
    (set-gl-impl this glEmulation)
    (update-framebuffer-info this)
    (.initiateGL this)
    (GLFW/glfwSetFramebufferSizeCallback (.getWindowHandle window)
                                         (.resizeCallback this))
    this))


(defn create-focus-callback[window]
  (proxy [GLFWWindowFocusCallback] []
    (invoke [window-handle focused?]
      (println "focused? " focused?)
      (.postRunnable window
                     (fn []
                       (let [lifecycleListeners (.lifecycleListeners window)]
                         (if focused?
                           (do
                            (when (:pauseWhenLostFocus (.cljConfig window))
                              (locking lifecycleListeners
                                (doseq [ll lifecycleListeners]
                                  (.resume ll)))
                              (.resume (.listener window)))
                            (when (.windowListener window)
                              (.focusGained (.windowListener window))))
                           (do
                            (when (.windowListener window)
                              (.focusLost (.windowListener window)))
                            (when (:pauseWhenLostFocus (.cljConfig window))
                              (locking lifecycleListeners
                                (doseq [ll lifecycleListeners]
                                  (.pause ll)))
                              (.pause (.listener window)))))
                         (set! (.focused window) focused?)))))))



(defn create-iconify-callback [window]
  (proxy [GLFWWindowIconifyCallback] []
    (invoke [window-handle iconified?]
      (println "iconified?" iconified?)
      (.postRunnable window
                     (fn []
                       (let [lifecycleListeners (.lifecycleListeners window)]
                         (when-let [wl (.windowListener window)]
                           (.iconified wl iconified?))
                         (set! (.iconified window) iconified?)
                         (when (:pauseWhenMinimized (.cljConfig window))
                           (if iconified?
                             (do
                              (locking lifecycleListeners
                                (doseq [ll lifecycleListeners]
                                  (.pause ll)))
                              (.pause (.listener window)))
                             (do
                              (locking lifecycleListeners
                                (doseq [ll lifecycleListeners]
                                  (.resume ll)))
                              (.resume (.listener window)))))))))))

(defn- createWindow* [application window sharedContext]
  (let [window-handle (createGlfwWindow (:glEmulation (:config application))
                                        (:config application)
                                        sharedContext)]
    (set! (.windowHandle window) window-handle)
    (set! (.input        window) (DefaultLwjgl3Input. window))
    (set! (.graphics     window) (create-graphics window (:glEmulation (:config application))))

		(let [callback (create-focus-callback window)]
      (set! (.focusCallback window) callback)
      (GLFW/glfwSetWindowFocusCallback    window-handle callback))

		(let [callback (create-iconify-callback window)]
      (set! (.iconifyCallback window) callback)
      (GLFW/glfwSetWindowIconifyCallback  window-handle callback))

		(GLFW/glfwSetWindowMaximizeCallback window-handle (.maximizeCallback window))
		(GLFW/glfwSetWindowCloseCallback    window-handle (.closeCallback    window))
		(GLFW/glfwSetDropCallback           window-handle (.dropCallback     window))
		(GLFW/glfwSetWindowRefreshCallback  window-handle (.refreshCallback  window))
    (when (.windowListener window)
      (.created (.windowListener window) window))
    (.setVisible window (:initialVisible (:config application)))
    (dotimes [_ 2]
      (.. window getGraphics getGL20
          (glClearColor (.r (:initialBackgroundColor (:config application)))
                        (.g (:initialBackgroundColor (:config application)))
                        (.b (:initialBackgroundColor (:config application)))
                        (.a (:initialBackgroundColor (:config application)))))
      (.. window getGraphics getGL20
          (glClear GL20/GL_COLOR_BUFFER_BIT))
      (GLFW/glfwSwapBuffers window-handle))
    (when @(:currentWindow application)
      ; the call above to createGlfwWindow switches the OpenGL context to the newly created window,
      ; ensure that the invariant "currentWindow is the window with the current active OpenGL context" holds
      (make-current! @(:currentWindow application)))))

(defn- createWindow [application listener sharedContext]
  (let [window (Lwjgl3Window. listener
                              (:lifecycleListeners application))]
    (set! (.windowListener window) (:windowListener (:config application)))
    (set! (.vSyncEnabled window) (:vSyncEnabled (:config application)))
    (set! (.cljConfig window) (:config application))
    (set! (.hdpiMode window)  (:hdpiMode (:config application)))
    (if (zero? sharedContext)
			; the main window is created immediately
      (createWindow* application window sharedContext)
			; creation of additional windows is deferred to avoid GL context trouble
      (.postRunnable application (fn []
                                   (createWindow* application window sharedContext)
                                   (swap! (:windows application) conj window))))
    window))

(defn- load-angle! []
  (eval '(com.badlogic.gdx.backends.lwjgl3.angle.ANGLELoader/load)))

(defn- post-load-angle! []
  (eval '(com.badlogic.gdx.backends.lwjgl3.angle.ANGLELoader/postGlfwInit)))

(defn- initialize-glfw! []
  (when (or (nil? errorCallback) (not (bound? #'errorCallback)))
    (Lwjgl3NativesLoader/load)
    (def errorCallback (GLFWErrorCallback/createPrint System/err))
    (GLFW/glfwSetErrorCallback errorCallback)
    (when (= SharedLibraryLoader/os Os/MacOsX)
      (GLFW/glfwInitHint GLFW/GLFW_ANGLE_PLATFORM_TYPE
                         GLFW/GLFW_ANGLE_PLATFORM_TYPE_METAL))
    (GLFW/glfwInitHint GLFW/GLFW_JOYSTICK_HAT_BUTTONS
                       GLFW/GLFW_FALSE)
    (assert (GLFW/glfwInit))))

(defn- main-loop! [application]
  (let [closedWindows (Array.)]
    (while (and @(:running? application)
                (> (count @(:windows application)) 0))
      (.update (:audio application)) ; FIXME put it on a separate thread
      (let [haveWindowsRendered (atom false)
            targetFramerate (atom -2)]
        (.clear closedWindows)
        (doseq [window @(:windows application)]
          (when-not (= @(:currentWindow application)
                       window)
            (make-current! window)
            (reset! (:currentWindow application) window))
          (when (= @targetFramerate -2)
            (reset! targetFramerate (:foreground-fps (:config application))))
          (swap! haveWindowsRendered #(or % (.update window)))
          (when (.shouldClose window)
            (.add closedWindows window)))
        (GLFW/glfwPollEvents)
        (let [shouldRequestRendering (atom (> (count @(:runnables application))
                                              0))]
          (reset! (:executedRunnables application) @(:runnables application))
          (reset! (:runnables application) [])
          (doseq [runnable @(:executedRunnables application)]
            (.run runnable))
          (when @shouldRequestRendering
            ; Must follow Runnables execution so changes done by Runnables are reflected
            ; in the following render.
            (doseq [window @(:windows application)
                    :when (not (.isContinuousRendering (.getGraphics window)))]
              (.requestRendering window)))

          (doseq [closedWindow closedWindows]
            (when (= (count @(:windows application)) 1)
              ; Lifecycle listener methods have to be called before ApplicationListener methods. The
              ; application will be disposed when _all_ windows have been disposed, which is the case,
              ; when there is only 1 window left, which is in the process of being disposed.
              (doseq [listener (:lifecycleListeners application)]
                (.pause listener)
                (.dispose listener))
              (.clear (:lifecycleListeners application)))
            (.dispose closedWindow)
            (swap! (:windows application) disj closedWindow))

          (if (not @haveWindowsRendered)
            ; Sleep a few milliseconds in case no rendering was requested
            ; with continuous rendering disabled.
            (try (Thread/sleep (/ 1000 (:idleFps (:config application))))
                 (catch InterruptedException e
                   ; ignore
                   ))
            (if (> @targetFramerate 0)
              ; sleep as needed to meet the target framerate
              (.sync (:sync application) @targetFramerate))))))))

(defn- cleanupWindows [application]
  (doseq [lifecycleListener (:lifecycleListeners application)]
    (.pause   lifecycleListener)
    (.dispose lifecycleListener))
  (doseq [window @(:windows application)]
    (.dispose window))
  (reset! (:windows application) #{}))

(defn- cleanup [application]
  (Lwjgl3Cursor/disposeSystemCursors)
  (.dispose (:audio application))
  (.free errorCallback)
  (def errorCallback nil)
  (when (and glDebugCallback (bound? #'glDebugCallback))
    (.free glDebugCallback)
    (def glDebugCallback nil))
  (GLFW/glfwTerminate))

(defrecord Lwjgl3Application [audio
                              config
                              current-window
                              files
                              log-level
                              logger
                              net
                              runnables
                              running?
                              lifecycleListeners]
  Application
  (getApplicationListener [_]
    (.getListener current-window))
  (getGraphics [_]
    (.getGraphics current-window))
  (getAudio [_]
    audio)
  (getInput [_]
    (.getInput current-window))
  (getFiles [_]
    files)
  (getNet [_]
    net)
  (debug [_ tag message]
    (when (>= log-level Application/LOG_DEBUG)
      (.debug logger tag message)))
  (debug [_ tag message exception]
    (when (>= log-level Application/LOG_DEBUG)
      (.debug logger tag message exception)))
  (log [_ tag message]
    (when (>= log-level Application/LOG_INFO)
      (.debug logger tag message))) ; FIXME debug
  (log [_ tag message exception]
    (when (>= log-level Application/LOG_INFO)
      (.debug logger tag message exception))) ; FIXME debug
  (error [_ tag message]
    (when (>= log-level Application/LOG_ERROR)
      (.debug logger tag message)))
  (error [_ tag message exception]
    (when (>= log-level Application/LOG_ERROR)
      (.debug logger tag message exception)))
  (setLogLevel [_ log-level]
    ;
    )
  (getLogLevel [_]
    log-level)
  (setApplicationLogger [_ logger]
    ;
    )
  (getApplicationLogger [_]
    logger)
  (getType [_]
    Application$ApplicationType/Desktop)
  (getVersion [_]
    0)
  (getJavaHeap [_]
    ; return Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
    )
  (getNativeHeap [_]
    ; getJavaHeap
    )
  (getPreferences [_ name]
	;	if (preferences.containsKey(name)) {
	;		return preferences.get(name);
	;	} else {
	;		Preferences prefs = new Lwjgl3Preferences(
	;			new Lwjgl3FileHandle(new File(config.preferencesDirectory, name), config.preferencesFileType));
	;		preferences.put(name, prefs);
	;		return prefs;
	;	}
    )
  (getClipboard [_]
    ;
    )
  (postRunnable [_ runnable]
    (swap! runnables conj runnable))
  (exit [_]
    (reset! running? false))
  (addLifecycleListener [_ listener]
    (.add lifecycleListeners listener))
  (removeLifecycleListener [_ listener]
    (.removeValue lifecycleListeners listener true)))

(def ^:private default-config
  {

   ;/** Sets whether to use vsync. This setting can be changed anytime at runtime via {@link Graphics#setVSync(boolean)}.
   ; *
   ; * For multi-window applications, only one (the main) window should enable vsync. Otherwise, every window will wait for the
   ; * vertical blank on swap individually, effectively cutting the frame rate to (refreshRate / numberOfWindows). */
   :vSyncEnabled true

   ; Sets the window title. If null, the application listener's class name is used.
   :title "foobar"
   ; Sets the initial background color. Defaults to black.
   :initialBackgroundColor com.badlogic.gdx.graphics.Color/BLACK
   ; visibility whether the window will be visible on creation. (default true)
   :initialVisible true
   :foreground-fps 60
   :disable-audio? false
   :max-net-threads Integer/MAX_VALUE
   :glEmulation :GLEmulation/GL20
   :gles30ContextMajorVersion 3
   :gles30ContextMinorVersion 2
   :audio {; the maximum number of sources that can be played simultaniously
           :audioDeviceSimultaneousSources 16
           ; the audio device buffer count
           :audioDeviceBufferCount 9
           ; the audio device buffer size in samples (default 512)
           :audioDeviceBufferSize 512}

   ; Sets the bit depth of the color, depth and stencil buffer as well as multi-sampling.
   :buffer-format {:r 8
                   :g 8
                   :b 8
                   :a 8
                   :depth 16
                   :stencil 0
                   :samples 0}

   ; Sets the polling rate during idle time in non-continuous rendering mode. Must be positive. Default is 60.
   ; FIXME unused !? theres a PR ?
   :idleFps 60

   ; Sets whether to pause the application {@link ApplicationListener#pause()} and fire
   ;* {@link LifecycleListener#pause()}/{@link LifecycleListener#resume()} events on when window is minimized/restored.
		:pauseWhenMinimized true

    ; Sets whether to pause the application {@link ApplicationListener#pause()} and fire
	 ; * {@link LifecycleListener#pause()}/{@link LifecycleListener#resume()} events on when window loses/gains focus.
    :pauseWhenLostFocus false

    ; Sets the directory where {@link Preferences} will be stored, as well as the file type to be used to store them. Defaults to
    ;* "$USER_HOME/.prefs/" and {@link FileType#External}.
    :preferencesDirectory ".prefs/"
    ;:preferencesFileType FileType.External

    ;	 Defines how HDPI monitors are handled. Operating systems may have a per-monitor HDPI scale setting. The operating system
    ;	  may report window width/height and mouse coordinates in a logical coordinate system at a lower resolution than the actual
    ;	  physical resolution. This setting allows you to specify whether you want to work in logical or raw pixel units. See
    ;	  {@link HdpiMode} for more information. Note that some OpenGL functions like {@link GL20#glViewport(int, int, int, int)} and
    ;	  {@link GL20#glScissor(int, int, int, int)} require raw pixel units. Use {@link HdpiUtils} to help with the conversion if
    ;	  HdpiMode is set to {@link HdpiMode#Logical}. Defaults to {@link HdpiMode#Logical}. */
    :hdpiMode HdpiMode/Logical

    ; Enables use of OpenGL debug message callbacks. If not supported by the core GL driver (since GL 4.3), this uses the
    ; KHR_debug, ARB_debug_output or AMD_debug_output extension if available. By default, debug messages with NOTIFICATION
    ;  severity are disabled to avoid log spam.
    ;
    ; You can call with {@link System#err} to output to the "standard" error output stream.
    ;
    ; Use {@link Lwjgl3Application#setGLDebugMessageControl(Lwjgl3Application.GLDebugMessageSeverity, boolean)} to enable or
    ;  disable other severity debug levels. */
    :debug false
    :debugStream System/err

    ; Sets the position of the window in windowed mode. Default -1 for both coordinates for centered on primary monitor.
    :windowX -1
    :windowY -1

    ; Sets the app to use windowed mode.
    :windowWidth 640
    :windowHeight 480

    ; Sets minimum and maximum size limits for the window. If the window is full screen or not resizable, these limits are
    ; * ignored. The default for all four parameters is -1, which means unrestricted
    :windowMinWidth -1
    :windowMinHeight -1
    :windowMaxWidth -1
    :windowMaxHeight -1

    ; resizable whether the windowed mode window is resizable (default true)
    :windowResizable true

    ; whether the windowed mode window is decorated, i.e. displaying the title bars (default true)
    :windowDecorated true

    ; whether the window starts maximized. Ignored if the window is full screen. (default false)
    :windowMaximized false

    ; what monitor the window should maximize to
    :maximizedMonitor nil ; Lwjgl3Graphics.Lwjgl3Monitor

    ; whether the window should automatically iconify and restore previous video mode on input focus loss.
    ; (default true) Does nothing in windowed mode.
    :autoIconify true

    ;	/** Sets the icon that will be used in the window's title bar. Has no effect in macOS, which doesn't use window icons.
    ;	 * @param fileType The type of file handle the paths are relative to.
    ;	 * @param filePaths One or more image paths, relative to the given {@linkplain FileType}. Must be JPEG, PNG, or BMP format. The
    ;	 *           one closest to the system's desired size will be scaled. Good sizes include 16x16, 32x32 and 48x48. */
    :windowIconFileType nil ; FileType.Internal
    :windowIconPaths []

    ; Sets the {@link Lwjgl3WindowListener} which will be informed about iconficiation, focus loss and window close events.
    :windowListener nil ; Lwjgl3WindowListener

    ; Sets the app to use fullscreen mode. Use the static methods like {@link Lwjgl3ApplicationConfiguration#getDisplayMode()} on
    ;* this class to enumerate connected monitors and their fullscreen display modes
    :fullscreenMode nil ; Lwjgl3DisplayMode
   }
  )

(defn application [listener config]
  (when (= (:glEmulation config) :GLEmulation/ANGLE_GLES20)
    (load-angle!))
  (.set Configuration/GLFW_LIBRARY_NAME "glfw_async")
  (initialize-glfw!)
  (let [config (merge default-config config)
        config (update config :title #(or % (.getSimpleName (.getClass listener))))
        config (assoc config
                      :windowWidth (:width (:windowed-mode config))
                      :windowHeight (:height (:windowed-mode config)))
        audio (if (:disable-audio? config)
                (MockAudio.)
                (OpenALLwjgl3Audio. (:audioDeviceSimultaneousSources (:audio config))
                                    (:audioDeviceBufferCount         (:audio config))
                                    (:audioDeviceBufferSize          (:audio config)))
                #_(try
                 (OpenALLwjgl3Audio. (:audioDeviceSimultaneousSources (:audio config))
                                     (:audioDeviceBufferCount         (:audio config))
                                     (:audioDeviceBufferSize          (:audio config)))
                 (catch Throwable t
                   #_(.log application "Lwjgl3Application", "Couldn't initialize audio, disabling audio", t)
                   #_(MockAudio.))))
        application (map->Lwjgl3Application
                     {:config config
                      :logger (create-logger)
                      :windows (atom #{})
                      :currentWindow (atom nil)
                      :audio audio
                      :files (files/create)
                      :net (Lwjgl3Net. (:max-net-threads config))
                      ;:preferences
                      :clipboard (create-clipboard)
                      :log-level Application/LOG_INFO
                      :running? (atom true)
                      :runnables (atom [])
                      :executedRunnables (atom [])
                      :lifecycleListeners (Array.)
                      :sync (Sync.)})]
    (set! Gdx/app application)
    (set! Gdx/audio (:audio application))
    (set! Gdx/files (:files application))
    (set! Gdx/net   (:net   application))
    (let [window (createWindow application listener 0)]
      (when (= (:glEmulation config) :GLEmulation/ANGLE_GLES20)
        (post-load-angle!))
      (swap! (:windows application) conj window))
    (try (main-loop!     application)
         (cleanupWindows application)
         (catch Throwable t
           (throw t))
         (finally
          (cleanup application)))))

(defn newWindow
  "Creates a new {@link Lwjgl3Window} using the provided listener and {@link Lwjgl3WindowConfiguration}.

  This function only just instantiates a {@link Lwjgl3Window} and returns immediately. The actual window creation is postponed
	 with {@link Application#postRunnable(Runnable)} until after all existing windows are updated."
  [application listener window-config]
  #_(let [appConfig (Lwjgl3ApplicationConfiguration/copy (:config application))]
    (.setWindowConfiguration appConfig window-config)
    ;(set! (.title appConfig) (.getSimpleName (.getClass listener)))
    (createWindow application
                  appConfig
                  listener
                  (.getWindowHandle (first @(:windows application))))))

#_(defn get-display-mode
  "the currently active {@link DisplayMode} of the primary monitor"
  []
  (let [monitor   (org.lwjgl.glfw.GLFW/glfwGetPrimaryMonitor)
        videoMode (org.lwjgl.glfw.GLFW/glfwGetVideoMode monitor)]
    (Lwjgl3Graphics$Lwjgl3DisplayMode.
     monitor
     (.width videoMode)
     (.height videoMode)
     (.refreshRate videoMode)
     (+ (.redBits videoMode)
        (.greenBits videoMode)
        (.blueBits videoMode)))))
