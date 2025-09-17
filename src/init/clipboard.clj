(ns init.clipboard
  (:import (com.badlogic.gdx.backends.lwjgl3 Lwjgl3Clipboard)))

(defn do! [{:keys [init/application]
            :as init }]
  (set! (.clipboard application) (Lwjgl3Clipboard.))
  init)
