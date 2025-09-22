(ns gdl.backends.desktop
  (:require [clojure.java.awt.taskbar]
            [com.badlogic.gdx.backends.lwjgl3 :as lwjgl]
            [com.badlogic.gdx.utils :as utils]
            [org.lwjgl.system.configuration]
            [gdl.backends.gdx.extends.audio]
            [gdl.backends.gdx.extends.files]
            [gdl.backends.gdx.extends.graphics]
            [gdl.backends.gdx.extends.input]))

(defn- set-mac-os-settings!
  [{:keys [glfw-async?
           taskbar-icon]}]
  (when glfw-async?
    (org.lwjgl.system.configuration/set-glfw-library-name! "glfw_async"))
  (when taskbar-icon
    (clojure.java.awt.taskbar/set-icon-image! taskbar-icon)))

(defn application
  [config]
  (when (= (utils/operating-system) :mac)
    (set-mac-os-settings! (:mac config)))
  (lwjgl/start-application! (:listener config)
                            (dissoc config :mac :listener)))
