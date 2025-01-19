(ns cdq.application.desktop
  (:require [clojure.gdx.application :as application]
            [clojure.gdx.backends.lwjgl :as lwjgl]
            [cdq.application]))

(defn start [{:keys [config
                     create
                     render
                     resize]}]
  (lwjgl/application (reify application/Listener
                       (create [_]
                         (cdq.application/create create))

                       (dispose [_]
                         (cdq.application/dispose))

                       (pause [_])

                       (render [_]
                         (cdq.application/render render))

                       (resize [_ width height]
                         (cdq.application/resize resize width height))

                       (resume [_]))
                     config))
