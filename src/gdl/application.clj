(ns gdl.application
  (:require cdq.utils
            [clojure.gdx.backends.lwjgl :as lwjgl]
            [clojure.gdx.utils.shared-library-loader :as shared-library-loader]
            [clojure.lwjgl.system.configuration]
            [clojure.java.awt.taskbar]
            [gdl.context])
  (:import (com.badlogic.gdx ApplicationListener)))

(defn- set-mac-settings! [{:keys [glfw-async? dock-icon]}]
  (when glfw-async?
    (clojure.lwjgl.system.configuration/set-glfw-library-name! "glfw_async"))
  (when dock-icon
    (clojure.java.awt.taskbar/set-icon! dock-icon)))

(defn- create-context [config]
  (assoc (gdl.context/create (::context config))
         :ctx/config config))

(defn -main [config-path]
  (let [{:keys [mac-os-settings lwjgl-app-config listener] :as config} (cdq.utils/load-edn-config config-path)]
    (when (= (shared-library-loader/os) :os/mac-osx)
            (set-mac-settings! mac-os-settings))
    (lwjgl/application lwjgl-app-config
                       (let [{:keys [create dispose render resize pause resume]} listener]
                         (proxy [ApplicationListener] []
                           (create  []              (when-let [[f params] create] (f (create-context config) params)))
                           (dispose []              (when dispose (dispose)))
                           (render  []              (when-let [[f params] render] (f params)))
                           (resize  [width height]  (when resize  (resize width height)))
                           (pause   []              (when pause   (pause)))
                           (resume  []              (when resume  (resume))))))))
