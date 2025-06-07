(ns gdl.mac-os
  (:require [clojure.java.awt.taskbar :as taskbar]
            [clojure.lwjgl.system.configuration :as lwjgl.system.configuration]))

(defn set-config! [{:keys [glfw-async?
                           dock-icon]}]
  (when glfw-async?
    (lwjgl.system.configuration/set-glfw-library-name! "glfw_async"))
  (when dock-icon
    (taskbar/set-icon! dock-icon)))
