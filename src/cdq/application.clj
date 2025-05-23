(ns cdq.application
  (:require [cdq.config :as config]
            [cdq.g :as g]
            [clojure.gdx.backends.lwjgl :as lwjgl])
  (:import (com.badlogic.gdx ApplicationAdapter)))

(def state (atom nil))

(defn -main []
  (let [config (config/create "config.edn")]
    (lwjgl/application (:clojure.gdx.backends.lwjgl config)
                       (proxy [ApplicationAdapter] []
                         (create []
                           (reset! state ((requiring-resolve (:create config)) config))
                           (g/validate @state))

                         (dispose []
                           (g/validate @state)
                           ((requiring-resolve (:dispose config)) @state))

                         (render []
                           (g/validate @state)
                           (swap! state (requiring-resolve (:render config)))
                           (g/validate @state))

                         (resize [_width _height]
                           (g/validate @state)
                           (g/update-viewports! @state))))))
