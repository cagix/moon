(ns com.badlogic.gdx.backends.lwjgl3.init.logger
  (:import (com.badlogic.gdx.backends.lwjgl3 Lwjgl3ApplicationLogger)))

(defn do!
  [{:keys [^com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application init/application]
    :as init}]
  (.setApplicationLogger application (Lwjgl3ApplicationLogger.))
  init)
