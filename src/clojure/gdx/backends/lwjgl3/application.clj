(ns clojure.gdx.backends.lwjgl3.application
  (:require [clojure.application :as app]
            [clojure.graphics]
            [clojure.files]
            [clojure.java.io :as io])
  (:import (java.awt Taskbar Toolkit)
           (com.badlogic.gdx ApplicationAdapter Gdx)
           (com.badlogic.gdx.backends.lwjgl3 Lwjgl3Application
                                             Lwjgl3ApplicationConfiguration)
           (com.badlogic.gdx.utils SharedLibraryLoader)
           (org.lwjgl.system Configuration)))

(defn- context []
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

(defn create [listener config]
  (.setIconImage (Taskbar/getTaskbar)
                 (.getImage (Toolkit/getDefaultToolkit)
                            (io/resource (:icon config))))
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

; oh now I know
; this belongs to 'clojure.gdx' ...

(extend-type com.badlogic.gdx.Files
  clojure.files/Files
  (internal [this path]
    (.internal this path)))

(extend-type com.badlogic.gdx.Graphics
  clojure.graphics/Graphics
  (delta-time [this]
    (.getDeltaTime this))
  (frames-per-second [this]
    (.getFramesPerSecond this))
  (new-cursor [this pixmap hotspot-x hotspot-y]
    (.newCursor this pixmap hotspot-x hotspot-y))
  (set-cursor [this cursor]
    (.setCursor this cursor)))

(extend-type com.badlogic.gdx.Application
  clojure.application/Application
  (exit [this]
    (.exit this))
  (post-runnable [this runnable]
    (.postRunnable this runnable)))
