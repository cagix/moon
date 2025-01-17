(ns cdq.application.desktop
  (:require [clojure.gdx.backends.lwjgl :as lwjgl]
            [clojure.utils :as utils]))

(defn start [{:keys [config listener]}]
  (lwjgl/application (proxy [com.badlogic.gdx.ApplicationAdapter] []
                       (create []
                         (utils/req-resolve-call (:create listener)))

                       (dispose []
                         (utils/req-resolve-call (:dispose listener)))

                       (render []
                         (utils/req-resolve-call (:render listener)))

                       (resize [width height]
                         (utils/req-resolve-call (:resize listener) width height)))
                     config))


