(ns com.badlogic.gdx.backends.lwjgl3.init.application
  (:import (com.badlogic.gdx.backends.lwjgl3 Lwjgl3Application)))

(defn do! [{:keys [init/config]
            :as init}]
  (assoc init :init/application (let [application (Lwjgl3Application.)]
                                  (set! (.config application) config)
                                  application)))
