(ns clojure.gdx.backends.lwjgl
  (:require [clojure.gdx.application :as application]
            [clojure.gdx.backends.lwjgl.application.configuration :as config]
            [clojure.gdx.backends.lwjgl.application.gl-debug-message-severity :as gl-debug-message-severity]
            [clojure.gdx.backends.lwjgl.window.configuration :as window-config])
  (:import (com.badlogic.gdx.backends.lwjgl3 Lwjgl3Application)))

(defn start-application! [listener config]
  (Lwjgl3Application. (application/listener listener)
                      (config/create config)))

(defn new-window! [application listener config]
  (Lwjgl3Application/.newWindow application
                                listener
                                (window-config/create config)))

(defn set-gl-debug-message-control! [severity enabled?]
  (Lwjgl3Application/setGLDebugMessageControl (gl-debug-message-severity/k->value severity)
                                              (boolean enabled?)))

(def display-mode!                 config/display-mode!)
(def display-modes!                config/display-modes!)
(def primary-monitor!              config/primary-monitor!)
(def monitors!                     config/monitors!)
