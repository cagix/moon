(ns init.net
  (:import (com.badlogic.gdx.backends.lwjgl3 Lwjgl3Net)))

(defn do! [{:keys [init/application
                   init/config]
            :as init}]
  (set! (.net application) (Lwjgl3Net. config))
  init)
