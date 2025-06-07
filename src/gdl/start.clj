(ns gdl.start
  (:require [cdq.utils]
            [clojure.gdx.backends.lwjgl :as lwjgl]
            [clojure.gdx.utils.shared-library-loader :as shared-library-loader]
            [clojure.gdx.utils.os :as os]
            [clojure.java.awt.taskbar :as taskbar]
            [clojure.lwjgl.system.configuration :as lwjgl.system.configuration])
  (:import (com.badlogic.gdx ApplicationListener)))

(defn- set-mac-os-config! [{:keys [glfw-async?
                                   dock-icon]}]
  (when glfw-async?
    (lwjgl.system.configuration/set-glfw-library-name! "glfw_async"))
  (when dock-icon
    (taskbar/set-icon! dock-icon)))

(defn application-adapter [{:keys [create dispose render resize pause resume]}]
  ; TODO validate possible combinations, e.g. typo 'resize!'
  (proxy [ApplicationListener] []
    (create  []              (when-let [[f params] create] (f params)))
    (dispose []              (when dispose (dispose)))
    (render  []              (when render  (render)))
    (resize  [width height]  (when resize  (resize width height)))
    (pause   []              (when pause   (pause)))
    (resume  []              (when resume  (resume)))))

(defn- operating-system []
  (get os/mapping (shared-library-loader/os)))

(defn -main [config-path]
  (let [config (cdq.utils/load-edn-config config-path)]
    (when (= (operating-system) :os/mac-osx)
      (set-mac-os-config! (:mac-os config)))
    (lwjgl/application! (:clojure.gdx.lwjgl/config config)
                        (application-adapter (:listener config)))))
