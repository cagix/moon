(ns cdq.game
  (:require [clojure.gdx.backends.lwjgl :as lwjgl]
            [gdl.application :as application]
            [gdl.utils :as utils])
  (:import (com.badlogic.gdx ApplicationAdapter)))

(defn -main [config-path]
  (let [config (utils/create-config config-path)]
    (lwjgl/application (:lwjgl-application config)
                       (proxy [ApplicationAdapter] []
                         (create []
                           (reset! application/state ((:create! config) config)))

                         (dispose []
                           ((:dispose! config) @application/state))

                         (render []
                           (swap! application/state (:render! config)))

                         (resize [width height]
                           ((:resize! config) @application/state width height))))))
