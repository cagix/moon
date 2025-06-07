(ns gdl.start
  (:require [clojure.edn :as edn]
            [clojure.gdx.backends.lwjgl :as lwjgl]
            [clojure.gdx.utils.shared-library-loader :as shared-library-loader]
            [clojure.gdx.utils.os :as os]
            [clojure.java.awt.taskbar :as taskbar]
            [clojure.java.io :as io]
            [clojure.lwjgl.system.configuration :as lwjgl.system.configuration])
  (:import (com.badlogic.gdx ApplicationListener)))

(defn- set-mac-os-config! [{:keys [glfw-async?
                                   dock-icon]}]
  (when glfw-async?
    (lwjgl.system.configuration/set-glfw-library-name! "glfw_async"))
  (when dock-icon
    (taskbar/set-icon! dock-icon)))

(defn- create-listener [config]
  (let [req-resolve-call (fn [k & params]
                           (when-let [f (k config)]
                             (apply (requiring-resolve f) params)))]
    (proxy [ApplicationListener] []
      (create  []             (req-resolve-call :clojure.gdx.lwjgl/create!))
      (dispose []             (req-resolve-call :clojure.gdx.lwjgl/dispose!))
      (render  []             (req-resolve-call :clojure.gdx.lwjgl/render!))
      (resize  [width height] (req-resolve-call :clojure.gdx.lwjgl/resize! width height))
      (pause   []             (req-resolve-call :clojure.gdx.lwjgl/pause!))
      (resume  []             (req-resolve-call :clojure.gdx.lwjgl/resume!)))))

(defn- operating-system []
  (get os/mapping (shared-library-loader/os)))

(defn- read-config [path]
  (-> path
      io/resource
      slurp
      edn/read-string))

(defn -main [config-path]
  (let [config (read-config config-path)]
    (when (= (operating-system) :os/mac-osx)
      (set-mac-os-config! (:mac-os config)))
    (lwjgl/application! (:clojure.gdx.lwjgl/config config)
                        (create-listener config))))
