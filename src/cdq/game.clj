(ns cdq.game
  (:require [clojure.gdx.backends.lwjgl :as lwjgl]
            [gdl.utils :as utils])
  (:import (com.badlogic.gdx ApplicationAdapter)))

(def state (atom nil))

(defn -main [config-path]
  (let [config (utils/create-config config-path)]
    (lwjgl/application (:lwjgl-application config)
                       (proxy [ApplicationAdapter] []
                         (create []
                           (reset! state ((:create! config) config)))

                         (dispose []
                           ((:dispose! config) @state))

                         (render []
                           (swap! state (:render! config)))

                         (resize [width height]
                           ((:resize! config) @state width height))))))
