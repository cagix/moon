(ns gdl.app
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io])
  (:import (com.badlogic.gdx ApplicationAdapter Gdx)
           (com.badlogic.gdx.backends.lwjgl3 Lwjgl3Application
                                             Lwjgl3ApplicationConfiguration)
           (com.badlogic.gdx.utils SharedLibraryLoader)
           (java.awt Taskbar Toolkit)
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

(def state (atom nil))

(defn post-runnable [f]
  (.postRunnable (:clojure.gdx/app @state) #(f @state)))

(defn start [config-path create dispose render resize]
  (let [config (-> config-path
                   io/resource
                   slurp
                   edn/read-string)]
    (.setIconImage (Taskbar/getTaskbar)
                   (.getImage (Toolkit/getDefaultToolkit)
                              (io/resource (:icon config))))
    (when SharedLibraryLoader/isMac
      (.set Configuration/GLFW_LIBRARY_NAME "glfw_async")
      #_(.set Configuration/GLFW_CHECK_THREAD0 false))
    (Lwjgl3Application. (proxy [ApplicationAdapter] []
                          (create []
                            (reset! state (create (context) (:context config))))

                          (dispose []
                            (dispose @state))

                          (render []
                            (swap! state render))

                          (resize [width height]
                            (resize @state width height)))
                        (doto (Lwjgl3ApplicationConfiguration.)
                          (.setTitle (:title config))
                          (.setWindowedMode (:window-width config)
                                            (:window-height config))
                          (.setForegroundFPS (:fps config))))))
