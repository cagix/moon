(ns gdl.app
  (:require [clojure.edn :as edn]
            [clojure.gdx :as gdx]
            [clojure.gdx.extend]
            [clojure.java.awt :as awt]
            [clojure.java.io :as io]
            [clojure.utils :refer [dispose disposable? resize resizable?]])
  (:import (com.badlogic.gdx Application
                             ApplicationAdapter)
           (com.badlogic.gdx.backends.lwjgl3 Lwjgl3Application
                                             Lwjgl3ApplicationConfiguration)
           (com.badlogic.gdx.utils SharedLibraryLoader)
           (org.lwjgl.system Configuration)))

(def state (atom nil))

(defn start* [create render config]
  (when-let [icon (:icon config)]
    (awt/set-taskbar-icon icon))
  (when (and SharedLibraryLoader/isMac
             (:glfw-async-on-mac-osx? config))
    (.set Configuration/GLFW_LIBRARY_NAME "glfw_async"))
  (Lwjgl3Application. (proxy [ApplicationAdapter] []
                        (create []
                          (reset! state (create (gdx/context) config)))

                        (dispose []
                          ; don't dispose internal classes (:clojure.gdx/graphics,etc. )
                          ; which Lwjgl3Application will handle
                          ; otherwise app crashed w. asset-manager
                          ; which was disposed after graphics
                          ; -> so there is a certain order to cleanup...
                          (doseq [[k value] @state
                                  :when (and (not (= (namespace k) "clojure.gdx"))
                                             (disposable? value))]
                            (when (:log-dispose-lifecycle? config)
                              (println "Disposing " k " - " value))
                            (dispose value)))

                        (render []
                          (swap! state render))

                        (resize [width height]
                          (doseq [[k value] @state
                                  :when (resizable? value)]
                            (when (:log-resize-lifecycle? config)
                              (println "Resizing " k " - " value))
                            (resize value width height))))
                      (doto (Lwjgl3ApplicationConfiguration.)
                        (.setTitle (:title config))
                        (.setWindowedMode (:width config) (:height config))
                        (.setForegroundFPS (:fps config)))))

(defn start
  ([create render]
   (start create render "config.edn"))
  ([create render edn-config]
   (start* create
           render
           (-> edn-config io/resource slurp edn/read-string))))

(defn post-runnable [f]
  (Application/.postRunnable (:clojure.gdx/app @state)
                             #(f @state)))
