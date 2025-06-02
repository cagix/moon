(ns clojure.start
  (:require [clojure.gdx.backends.lwjgl :as lwjgl]
            [clojure.utils :as utils])
  (:import (com.badlogic.gdx ApplicationAdapter)))

(defn -main [config-path]
  (let [config (utils/load-edn-config config-path)]
    (lwjgl/application (:clojure.gdx.backends.lwjgl/application config)
                       (proxy [ApplicationAdapter] []
                         (create []
                           ((:create! config) config))

                         (dispose []
                           ((:dispose! config)))

                         (render []
                           ((:render! config)))

                         (resize [width height]
                           ((:resize! config) width height))))))
