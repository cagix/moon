(ns gdl.start
  (:require [clojure.gdx.app-listener :as app-listener]
            [clojure.gdx.backends.lwjgl :as lwjgl]
            [clojure.gdx.utils.shared-library-loader :as shared-library-loader]
            [clojure.gdx.utils.os :as os]
            [clojure.java.awt.taskbar :as taskbar]
            [clojure.lwjgl.system.configuration :as lwjgl.system.configuration]))

(defn- set-mac-os-config! [{:keys [glfw-async?
                                   dock-icon]}]
  (when glfw-async?
    (lwjgl.system.configuration/set-glfw-library-name! "glfw_async"))
  (when dock-icon
    (taskbar/set-icon! dock-icon)))

(defn- operating-system []
  (get os/mapping (shared-library-loader/os)))

(defn start! [config]
  (when (= (operating-system) :os/mac-osx)
    (set-mac-os-config! (:mac-os config)))
  (lwjgl/application! (:clojure.gdx.lwjgl/config config)
                      (app-listener/create-adapter (:listener config))))
