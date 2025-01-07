(ns clojure.gdx.backends.lwjgl3.application
  "Provides an interface for creating and managing a LibGDX application using the LWJGL3 backend.
   This namespace includes functionality for initializing the application with a listener and configuration,
   as well as accessing the LibGDX global context."
  (:require [clojure.gdx.application :as app]
            [clojure.java.io :as io])
  (:import (java.awt Taskbar Toolkit)
           (com.badlogic.gdx ApplicationAdapter Gdx)
           (com.badlogic.gdx.backends.lwjgl3 Lwjgl3Application
                                             Lwjgl3ApplicationConfiguration)
           (com.badlogic.gdx.utils SharedLibraryLoader)
           (org.lwjgl.system Configuration)))

(defn- context
  "Returns a map of the LibGDX global context.
   Call this function after the application's `create` method is executed.
   The returned map provides convenient access to key components of the LibGDX framework,
   such as graphics, audio, input, and file handling."
  []
  {:clojure.gdx/app      Gdx/app
   :clojure.gdx/audio    Gdx/audio
   :clojure.gdx/files    Gdx/files
   :clojure.gdx/gl       Gdx/gl
   :clojure.gdx/gl20     Gdx/gl20
   :clojure.gdx/gl30     Gdx/gl30
   :clojure.gdx/gl31     Gdx/gl31
   :clojure.gdx/gl32     Gdx/gl32
   :clojure.gdx/graphics Gdx/graphics
   :clojure.gdx/input    Gdx/input
   :clojure.gdx/net      Gdx/net})

; TODO so what we want to achieve now is to not set 'Gdx'
; so we have a stateless libgdx
; => or rather do not expose functions which use Gdx internally
; => can make PR's to libgdx to over methods w/o global state
; or just port it
; 1. of all don't set! the Gdx and see what breaks
; fix that only

(defn create
  "Initializes and launches a LibGDX application using the LWJGL3 backend.

   - `listener`: A record or object implementing the necessary application lifecycle methods
     (`create`, `dispose`, `pause`, `render`, `resize`, and `resume`) to handle game logic.
   - `config`: A map containing configuration options for the application, including:
       - `:title` (string): The title of the application window.
       - `:window-width` (int): The width of the application window.
       - `:window-height` (int): The height of the application window.
       - `:fps` (int): The desired frames per second for the application.
       - `:icon` (string): Path to the icon image resource for the application.

   This function sets up the LWJGL3 application with the specified configuration,
   applies platform-specific settings (e.g., for macOS), and proxies lifecycle events
   to the provided listener."
  [listener config]
  (.setIconImage (Taskbar/getTaskbar)
                 (.getImage (Toolkit/getDefaultToolkit)
                            (io/resource (:icon config))))
  (when SharedLibraryLoader/isMac
    (.set Configuration/GLFW_LIBRARY_NAME "glfw_async")
    (.set Configuration/GLFW_CHECK_THREAD0 false))
  (Lwjgl3Application. (proxy [ApplicationAdapter] []
                        (create []
                          (app/create listener (context)))
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


;  import java.util.Arrays;
;  import com.badlogic.gdx.Files.FileType;
;  import com.badlogic.gdx.Graphics;
;  import com.badlogic.gdx.Graphics.DisplayMode;
;  import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Graphics.Lwjgl3DisplayMode;
;  import com.badlogic.gdx.graphics.Color;

; {:window-x -1
;  :window-y -1
;  :window-width 640 ; width the width of the window (default 640)
;  :window-height 480 ; height the height of the window (default 480)
;  :window-min-width -1
;  :windowMinHeight -1,
;  :windowMaxWidth = -1,
;  :windowMaxHeight = -1;
;  :window-resizable? true ; whether the windowed mode window is resizable (default true)
;  :window-decorated? true
;  :window-maximized? false
;  :maximized-monitor
;  :auto-iconify? true
;  :window-icon-file-type
;  :window-icon-paths
;  :window-listener
;  :fullscreen-mode
;  :title
;  :initial-background-color Color/BLACK
;  :initial-visible? true ; visibility whether the window will be visible on creation. (default true)
;  :vsync-enabled? true
;  }

;  	/** @param resizable whether the windowed mode window is resizable (default true) */
;  	public void setResizable (boolean resizable) {
;  		this.windowResizable = resizable;
;  	}
;
;  	/** @param decorated whether the windowed mode window is decorated, i.e. displaying the title bars (default true) */
;  	public void setDecorated (boolean decorated) {
;  		this.windowDecorated = decorated;
;  	}
;
;  	/** @param maximized whether the window starts maximized. Ignored if the window is full screen. (default false) */
;  	public void setMaximized (boolean maximized) {
;  		this.windowMaximized = maximized;
;  	}
;
;  	/** @param monitor what monitor the window should maximize to */
;  	public void setMaximizedMonitor (Graphics.Monitor monitor) {
;  		this.maximizedMonitor = (Lwjgl3Graphics.Lwjgl3Monitor)monitor;
;  	}
;
;  	/** @param autoIconify whether the window should automatically iconify and restore previous video mode on input focus loss.
;  	 *           (default true) Does nothing in windowed mode. */
;  	public void setAutoIconify (boolean autoIconify) {
;  		this.autoIconify = autoIconify;
;  	}
;
;  	/** Sets the position of the window in windowed mode. Default -1 for both coordinates for centered on primary monitor. */
;  	public void setWindowPosition (int x, int y) {
;  		windowX = x;
;  		windowY = y;
;  	}
;
;  	/** Sets minimum and maximum size limits for the window. If the window is full screen or not resizable, these limits are
;  	 * ignored. The default for all four parameters is -1, which means unrestricted. */
;  	public void setWindowSizeLimits (int minWidth, int minHeight, int maxWidth, int maxHeight) {
;  		windowMinWidth = minWidth;
;  		windowMinHeight = minHeight;
;  		windowMaxWidth = maxWidth;
;  		windowMaxHeight = maxHeight;
;  	}
;
;  	/** Sets the icon that will be used in the window's title bar. Has no effect in macOS, which doesn't use window icons.
;  	 * @param filePaths One or more {@linkplain FileType#Internal internal} image paths. Must be JPEG, PNG, or BMP format. The one
;  	 *           closest to the system's desired size will be scaled. Good sizes include 16x16, 32x32 and 48x48. */
;  	public void setWindowIcon (String... filePaths) {
;  		setWindowIcon(FileType.Internal, filePaths);
;  	}
;
;  	/** Sets the icon that will be used in the window's title bar. Has no effect in macOS, which doesn't use window icons.
;  	 * @param fileType The type of file handle the paths are relative to.
;  	 * @param filePaths One or more image paths, relative to the given {@linkplain FileType}. Must be JPEG, PNG, or BMP format. The
;  	 *           one closest to the system's desired size will be scaled. Good sizes include 16x16, 32x32 and 48x48. */
;  	public void setWindowIcon (FileType fileType, String... filePaths) {
;  		windowIconFileType = fileType;
;  		windowIconPaths = filePaths;
;  	}
;
;  	/** Sets the {@link Lwjgl3WindowListener} which will be informed about iconficiation, focus loss and window close events. */
;  	public void setWindowListener (Lwjgl3WindowListener windowListener) {
;  		this.windowListener = windowListener;
;  	}
;
;  	/** Sets the app to use fullscreen mode. Use the static methods like {@link Lwjgl3ApplicationConfiguration#getDisplayMode()} on
;  	 * this class to enumerate connected monitors and their fullscreen display modes. */
;  	public void setFullscreenMode (DisplayMode mode) {
;  		this.fullscreenMode = (Lwjgl3DisplayMode)mode;
;  	}
;
;  	/** Sets the window title. If null, the application listener's class name is used. */
;  	public void setTitle (String title) {
;  		this.title = title;
;  	}
;
;  	/** Sets the initial background color. Defaults to black. */
;  	public void setInitialBackgroundColor (Color color) {
;  		initialBackgroundColor = color;
;  	}
;
;  	/** Sets whether to use vsync. This setting can be changed anytime at runtime via {@link Graphics#setVSync(boolean)}.
;  	 *
;  	 * For multi-window applications, only one (the main) window should enable vsync. Otherwise, every window will wait for the
;  	 * vertical blank on swap individually, effectively cutting the frame rate to (refreshRate / numberOfWindows). */
;  	public void useVsync (boolean vsync) {
;  		this.vSyncEnabled = vsync;
;  	}
;  }
;
