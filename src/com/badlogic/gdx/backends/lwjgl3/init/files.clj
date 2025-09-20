(ns com.badlogic.gdx.backends.lwjgl3.init.files
  (:import (com.badlogic.gdx.backends.lwjgl3 Lwjgl3Files)))

(defn do! [{:keys [^com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application init/application]
            :as init}]
  (set! (.files application) (Lwjgl3Files.))
  init)
