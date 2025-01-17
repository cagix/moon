(ns cdq.application.desktop
  (:require [clojure.app :as app]
            [clojure.application]
            [clojure.gdx.backends.lwjgl :as lwjgl]))

(defn start [{:keys [config
                     create
                     render
                     resize]}]
  (lwjgl/application (reify app/Listener
                       (create [_]
                         (clojure.application/create create))

                       (dispose [_]
                         (clojure.application/dispose))

                       (pause [_])

                       (render [_]
                         (clojure.application/render render))

                       (resize [_ width height]
                         (clojure.application/resize resize width height))

                       (resume [_]))
                     config))
