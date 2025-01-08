(ns clojure.gdx.backends.lwjgl3.application
  "Provides an interface for creating and managing a LibGDX application using the LWJGL3 backend.
   This namespace includes functionality for initializing the application with a listener and configuration,
   as well as accessing the LibGDX global context."
  (:require [clojure.application :as app]
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
  ; The API does _not_ do that !
  ; just write an API - neither go into libgdx now nor into your game -
  ; stay in the middle - not up or down -
  ; a complete API
  ; start with the classes you are using -
  (.setIconImage (Taskbar/getTaskbar)
                 (.getImage (Toolkit/getDefaultToolkit)
                            (io/resource (:icon config))))
  ; but what you could also do , move these 2 lines into libgdx ... ! its java code !
  (when SharedLibraryLoader/isMac
    (.set Configuration/GLFW_LIBRARY_NAME "glfw_async")
    #_(.set Configuration/GLFW_CHECK_THREAD0 false))
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
