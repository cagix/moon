(ns clojure.gdx.backends.lwjgl3
  (:require [clojure.gdx.application.listener :as listener])
  (:import (com.badlogic.gdx.backends.lwjgl3 Lwjgl3Application
                                             Lwjgl3ApplicationConfiguration
                                             Lwjgl3WindowConfiguration)))


(defn- set-window-config-key!
  [^Lwjgl3WindowConfiguration object k v]
  (case k
    :windowed-mode (.setWindowedMode object (int (:width v)) (int (:height v)))
    :title         (.setTitle        object (str v))))

(defn- set-application-config-key!
  [^Lwjgl3ApplicationConfiguration object k v]
  (case k
    :foreground-fps (.setForegroundFPS object (int v))
    (set-window-config-key! object k v)))

(defn start-application!
  [listener config]
  (Lwjgl3Application. (listener/create listener)
                      (let [obj (Lwjgl3ApplicationConfiguration.)]
                        (doseq [[k v] config]
                          (set-application-config-key! obj k v))
                        obj)))
