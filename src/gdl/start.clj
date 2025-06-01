(ns gdl.start
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.walk :as walk])
  (:import (com.badlogic.gdx ApplicationAdapter)
           (com.badlogic.gdx.backends.lwjgl3 Lwjgl3Application
                                             Lwjgl3ApplicationConfiguration)
           (com.badlogic.gdx.utils SharedLibraryLoader
                                   Os)
           (java.awt Taskbar
                     Toolkit)
           (org.lwjgl.system Configuration)))

(defn- require-symbols [form]
  (walk/postwalk (fn [form]
                   (if (symbol? form)
                     (if (namespace form)
                       (requiring-resolve form)
                       (do (require form) form))
                     form))
                 form))

(defn- create-config [path]
  (let [m (->> path
               io/resource
               slurp
               edn/read-string
               require-symbols)]
    (reify clojure.lang.ILookup
      (valAt [_ k]
        (assert (contains? m k)
                (str "Config key not found: " k))
        (get m k)))))

(defn- set-mac-os-config! [config]
  (when (:glfw-async? config)
    (.set Configuration/GLFW_LIBRARY_NAME "glfw_async"))
  (when-let [icon (:dock-icon config)]
    (.setIconImage (Taskbar/getTaskbar)
                   (.getImage (Toolkit/getDefaultToolkit)
                              (io/resource icon)))))

(defn- lwjgl3-config [config]
  (doto (Lwjgl3ApplicationConfiguration.)
    (.setTitle (:title config))
    (.setWindowedMode (:width (:windowed-mode config))
                      (:height (:windowed-mode config)))
    (.setForegroundFPS (or (:foreground-fps config) 60))))

(defn -main [config-path]
  (let [config (create-config config-path)]
    (when (= SharedLibraryLoader/os Os/MacOsX)
      (set-mac-os-config! (:mac-os config)))
    (Lwjgl3Application. (proxy [ApplicationAdapter] []
                          (create []
                            ((:create! config) config))

                          (dispose []
                            ((:dispose! config)))

                          (render []
                            ((:render! config)))

                          (resize [width height]
                            ((:resize! config) width height)))
                        (lwjgl3-config (:lwjgl3-config config)))))
