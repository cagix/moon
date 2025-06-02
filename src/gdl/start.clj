(ns gdl.start
  (:require [clojure.gdx.backends.lwjgl :as lwjgl]
            [gdl.utils :as utils])
  (:import (com.badlogic.gdx ApplicationAdapter)))

(defn -main [config-path]
  (let [config (utils/load-edn-config config-path)]
    (lwjgl/application (:lwjgl3-config config)
                       (proxy [ApplicationAdapter] []
                         (create []
                           ((:create! config) config))

                         (dispose []
                           ((:dispose! config)))

                         (render []
                           ((:render! config)))

                         (resize [width height]
                           ((:resize! config) width height))))))
