(ns cdq.start
  (:require [clojure.edn :as edn]
            [clojure.gdx.backends.lwjgl :as lwjgl]
            [clojure.java.io :as io])
  (:import (com.badlogic.gdx ApplicationListener)
           (com.badlogic.gdx.utils SharedLibraryLoader)
           (java.awt Taskbar
                     Toolkit)
           (org.lwjgl.system Configuration)))

(defn- set-glfw-async! []
  (.set Configuration/GLFW_LIBRARY_NAME "glfw_async"))

(defn- set-taskbar-icon! [io-resource]
  (.setIconImage (Taskbar/getTaskbar)
                 (.getImage (Toolkit/getDefaultToolkit)
                            (io/resource io-resource))))

(defn- operating-system []
  (let [os SharedLibraryLoader/os]
    (cond (= os Os/MacOsX) :operating-system/mac)))

(defn- set-mac-os-config! [{:keys [glfw-async?
                                   dock-icon]}]
  (when glfw-async?
    (set-glfw-async!))
  (when dock-icon
    (set-taskbar-icon! dock-icon)))

(defn -main [app-edn-path]
  (let [config (-> app-edn-path
                   io/resource
                   slurp
                   edn/read-string)
        req-resolve-call (fn [k & params]
                           (when-let [f (k config)]
                             (apply (requiring-resolve f) params)))]
    (when (= (operating-system) :operating-system/mac)
      (set-mac-os-config! (:mac-os config)))
    (lwjgl/application! (:clojure.gdx.lwjgl/config config)
                        (proxy [ApplicationListener] []
                          (create  []             (req-resolve-call :clojure.gdx.lwjgl/create! config))
                          (dispose []             (req-resolve-call :clojure.gdx.lwjgl/dispose!))
                          (render  []             (req-resolve-call :clojure.gdx.lwjgl/render!))
                          (resize  [width height] (req-resolve-call :clojure.gdx.lwjgl/resize! width height))
                          (pause   []             (req-resolve-call :clojure.gdx.lwjgl/pause!))
                          (resume  []             (req-resolve-call :clojure.gdx.lwjgl/resume!))))))
