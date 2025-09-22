(ns com.badlogic.gdx.backends.lwjgl3.application.config
  (:require [com.badlogic.gdx.backends.lwjgl3.window.config :as window-config])
  (:import (com.badlogic.gdx.backends.lwjgl3 Lwjgl3ApplicationConfiguration)))

(defn create [config]
  (let [obj (Lwjgl3ApplicationConfiguration.)]
    (doseq [[k v] config]
      (case k
        :foreground-fps (.setForegroundFPS obj (int v))
        (window-config/set-option! obj k v)))
    obj))
