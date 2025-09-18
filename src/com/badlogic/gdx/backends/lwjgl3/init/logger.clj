(ns com.badlogic.gdx.backends.lwjgl3.init.logger
  (:import (com.badlogic.gdx.backends.lwjgl3 Lwjgl3ApplicationLogger)))

(defn do!
  [{:keys [init/application]
    :as init}]
  (.setApplicationLogger application (Lwjgl3ApplicationLogger.))
  init)
