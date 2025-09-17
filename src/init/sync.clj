(ns init.sync
  (:import (com.badlogic.gdx.backends.lwjgl3 Sync)))

(defn do! [{:keys [init/application]
            :as init}]
  (set! (.sync application) (Sync.))
  init)
