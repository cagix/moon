(ns cdq.application
  (:require [clojure.gdx :as gdx]
            [clojure.gdx.backends.lwjgl :as lwjgl])
  (:import (com.badlogic.gdx ApplicationAdapter)))

(def state (atom nil))

(defn start! [{::keys [create!
                       dispose!
                       render!
                       resize!
                       validate-ctx-schema
                       lwjgl-app-config]
               :as config}]
  (lwjgl/application lwjgl-app-config
                     (proxy [ApplicationAdapter] []
                       (create []
                         (reset! state (create! (gdx/get-context) config))
                         (validate-ctx-schema @state))

                       (dispose []
                         (dispose! @state))

                       (render []
                         (validate-ctx-schema @state)
                         (swap! state render!)
                         (validate-ctx-schema @state))

                       (resize [width height]
                         (resize! @state width height)))))
