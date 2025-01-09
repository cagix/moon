(ns gdl.app
  (:require [clojure.edn :as edn]
            [clojure.gdx]
            [clojure.input :as input]
            [clojure.java.io :as io]
            [cdq.db :as db]
            [gdl.graphics :as graphics]
            [gdl.ui :as ui])
  (:import (com.badlogic.gdx ApplicationAdapter Gdx)
           (com.badlogic.gdx.backends.lwjgl3 Lwjgl3Application
                                             Lwjgl3ApplicationConfiguration)
           (com.badlogic.gdx.utils SharedLibraryLoader)
           (java.awt Taskbar Toolkit)
           (org.lwjgl.system Configuration)))

(defn gdl-context [config]
  (let [gdx {:clojure.gdx/app      Gdx/app
             :clojure.gdx/files    Gdx/files
             :clojure.gdx/graphics Gdx/graphics
             :clojure.gdx/input    Gdx/input}
        g (graphics/create gdx (:graphics config))
        batch (:batch g)
        shape-drawer (:sd g)
        sd-texture (:sd-texture g)
        cursors (:cursors g)
        default-font (:default-font g)
        tiled-map-renderer (:tiled-map-renderer g)
        world-unit-scale (:world-unit-scale g)
        world-viewport (:world-viewport g)
        ui-viewport (:ui-viewport g)

        _ (ui/load! (:ui config))
        stage (ui/stage ui-viewport batch nil)
        _ (input/set-processor (:clojure.gdx/input gdx) stage)

        ]
    (merge gdx
           {:gdl.context/batch batch
            :gdl.context/cursors cursors
            :gdl.context/default-font default-font
            :gdl.context/shape-drawer shape-drawer
            :gdl.context/sd-texture sd-texture
            :gdl.context/db (db/create (:db config))
            :gdl.context/stage stage
            :gdl.context/viewport ui-viewport
            :gdl.context/world-viewport world-viewport
            :gdl.context/world-unit-scale world-unit-scale
            :gdl.context/tiled-map-renderer tiled-map-renderer})))

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
                            (reset! state
                                    (create (gdl-context config)
                                            (:context config))))

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
