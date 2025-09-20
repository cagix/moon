(ns com.badlogic.gdx.backends.lwjgl3.init.sync
  (:import (com.badlogic.gdx.backends.lwjgl3 Sync)))

(defn do! [{:keys [^com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application init/application]
            :as init}]
  (set! (.sync application) (Sync.))
  init)
