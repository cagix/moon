(ns com.badlogic.gdx.backends.lwjgl3.init.clipboard
  (:import (com.badlogic.gdx.backends.lwjgl3 Lwjgl3Clipboard)))

(defn do! [{:keys [^com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application init/application]
            :as init}]
  (set! (.clipboard application) (Lwjgl3Clipboard.))
  init)
