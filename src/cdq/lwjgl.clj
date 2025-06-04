(ns cdq.lwjgl
  (:require [cdq.utils :as utils]
            [clojure.application-listener :as application]
            [clojure.gdx.lwjgl :as lwjgl]))

(defn invoke [[f params]]
  (f params))

(defn -main [config-path]
  (let [config (utils/load-edn-config config-path)]
    (lwjgl/start-application! (reify application/Listener
                                (create! [_]
                                  ((::create! config) config))
                                (dispose! [_]
                                  ((::dispose! config)))
                                (render! [_]
                                  (invoke (::render! config)))
                                (resize! [_ width height]
                                  ((::resize! config) width height))
                                (pause! [_])
                                (resume! [_]))
                              (::config config))))
