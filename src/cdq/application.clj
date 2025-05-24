(ns cdq.application
  (:require [cdq.config :as config]
            [clojure.gdx.backends.lwjgl :as lwjgl])
  (:import (com.badlogic.gdx ApplicationAdapter)))

(def state (atom nil))

(defn -main []
  (let [config (config/create "config.edn")
        create!             (requiring-resolve (::create!              config))
        dispose!            (requiring-resolve (::dispose!             config))
        render!             (requiring-resolve (::render!              config))
        resize!             (requiring-resolve (::resize!              config))
        validate-ctx-schema (requiring-resolve (::validate-ctx-schema  config))]
    (lwjgl/application (::clojure.gdx.backends.lwjgl config)
                       (proxy [ApplicationAdapter] []
                         (create []
                           (reset! state (create! config))
                           (validate-ctx-schema @state))

                         (dispose []
                           (dispose! @state))

                         (render []
                           (validate-ctx-schema @state)
                           (swap! state render!)
                           (validate-ctx-schema @state))

                         (resize [width height]
                           (resize! @state width height))))))
